package net.tapacywka.aacs;

import com.google.gson.JsonParser;
import net.tapacywka.aacs.api.BoolValue;
import net.tapacywka.aacs.api.ConfigFile;
import net.tapacywka.aacs.api.DoubleValue;
import net.tapacywka.aacs.api.EnumValue;
import net.tapacywka.aacs.api.IntValue;
import net.tapacywka.aacs.api.ListValue;
import net.tapacywka.aacs.api.StringValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The settings file: values by reference, saved and read back, bad values forgiven, unknown keys kept. */
class ConfigFileTest {
    private enum Shape { SQUARE, ROUND }

    private enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private enum Detail { LOW, MEDIUM, HIGH }

    private static final class Spec {
        final ConfigFile file;
        BoolValue on;
        IntValue radius;
        DoubleValue zoom;
        EnumValue<Shape> shape;
        StringValue theme;
        ListValue hidden;

        Spec() {
            ConfigFile.Builder b = ConfigFile.builder();
            b.section("minimap", s -> {
                on = s.bool("enabled", true);
                radius = s.integer("radius", 48, 24, 110);
                zoom = s.decimal("zoom", 1.0, 1 / 32.0, 32.0);
                shape = s.choice("shape", Shape.SQUARE);
            });
            b.push("appearance");
            theme = b.text("theme", "default", t -> t.matches("[a-z0-9_.-]+"));
            b.push("map");
            hidden = b.list("plantsHidden", List.of(), id -> id.contains(":"));
            b.pop().pop();
            file = b.build();
        }
    }

    @Test
    void noFileGivesTheDefaultsAndWritesThem(@TempDir Path dir) {
        Spec s = new Spec();
        Path json = dir.resolve("mymod-client.json");
        assertFalse(s.file.load(json), "there was no file");
        assertEquals(48, s.radius.get());
        assertTrue(Files.exists(json), "written at once");
        assertEquals(List.of("minimap", "radius"), s.radius.path());
        assertEquals("appearance.map.plantsHidden", s.hidden.key());
    }

    @Test
    void aChangedValueIsSavedAndReadBack(@TempDir Path dir) {
        Path json = dir.resolve("c.json");
        Spec s = new Spec();
        s.file.load(json);
        s.radius.set(100);
        s.shape.set(Shape.ROUND);
        s.theme.set("my_pack.theme");
        s.hidden.set(List.of("minecraft:fern", "minecraft:short_grass"));
        s.file.save();
        Spec read = new Spec();
        assertTrue(read.file.load(json));
        assertEquals(100, read.radius.get());
        assertEquals(Shape.ROUND, read.shape.get());
        assertEquals("my_pack.theme", read.theme.get());
        assertEquals(List.of("minecraft:fern", "minecraft:short_grass"), read.hidden.get());
    }

    @Test
    void settingBringsNumbersIntoRangeAndRefusesTheImpossible() {
        Spec s = new Spec();
        s.radius.set(500);
        assertEquals(110, s.radius.get());
        s.zoom.set(-1.0);
        assertEquals(1 / 32.0, s.zoom.get());
        assertThrows(IllegalArgumentException.class, () -> s.theme.set("Bad Theme!"));
        assertEquals("default", s.theme.get(), "unchanged");
        s.radius.reset();
        assertTrue(s.radius.isDefault());
    }

    @Test
    void badValuesInTheFileAreForgiven(@TempDir Path dir) throws IOException {
        Path json = dir.resolve("c.json");
        Files.writeString(json, """
                { "minimap": { "enabled": "yes", "radius": 500, "zoom": 2.5, "shape": "round" },
                  "appearance": { "theme": "Bad Theme!", "map": { "plantsHidden": ["minecraft:fern", "nope", 3] } } }
                """, StandardCharsets.UTF_8);
        Spec s = new Spec();
        s.file.load(json);
        assertTrue(s.on.get(), "wrong kind: the default");
        assertEquals(110, s.radius.get(), "out of range: brought into it");
        assertEquals(2.5, s.zoom.get(), "in range: kept");
        assertEquals(Shape.ROUND, s.shape.get(), "the case does not matter");
        assertEquals("default", s.theme.get(), "not accepted: the default");
        assertEquals(List.of("minecraft:fern"), s.hidden.get(), "the entries not accepted left out");
    }

    @Test
    void anUnknownChoiceOrAFractionBecomesTheDefault(@TempDir Path dir) throws IOException {
        Path json = dir.resolve("c.json");
        Files.writeString(json, "{ \"minimap\": { \"radius\": 50.5, \"shape\": \"TRIANGLE\" } }", StandardCharsets.UTF_8);
        Spec s = new Spec();
        s.file.load(json);
        assertEquals(48, s.radius.get());
        assertEquals(Shape.SQUARE, s.shape.get());
    }

    @Test
    void unknownKeysStayInTheFile(@TempDir Path dir) throws IOException {
        Path json = dir.resolve("c.json");
        Files.writeString(json, """
                { "minimap": { "radius": 60, "newerSetting": 7 }, "other": { "a": [1, 2] } }
                """, StandardCharsets.UTF_8);
        Spec s = new Spec();
        s.file.load(json);
        s.radius.set(70);
        s.file.save();
        var root = JsonParser.parseString(Files.readString(json)).getAsJsonObject();
        assertEquals(70, root.getAsJsonObject("minimap").get("radius").getAsInt());
        assertEquals(7, root.getAsJsonObject("minimap").get("newerSetting").getAsInt());
        assertEquals(2, root.getAsJsonObject("other").getAsJsonArray("a").size());
    }

    @Test
    void aBrokenFileIsKeptBesideTheDefaults(@TempDir Path dir) throws IOException {
        Path json = dir.resolve("c.json");
        Files.writeString(json, "{ \"minimap\": { \"radius\": ", StandardCharsets.UTF_8);
        Spec s = new Spec();
        s.file.load(json);
        assertEquals(48, s.radius.get());
        assertEquals("{ \"minimap\": { \"radius\": ", Files.readString(dir.resolve("c.json.broken")));
    }

    @Test
    void listenersHearEachChangeOnceAndTheGenerationGoesUp() {
        Spec s = new Spec();
        List<String> heard = new ArrayList<>();
        s.radius.onChange((before, after) -> heard.add(before + ">" + after));
        int g = s.file.generation();
        s.radius.set(60);
        s.radius.set(60);
        s.radius.reset();
        assertEquals(List.of("48>60", "60>48"), heard, "a set to the same value is no change");
        assertEquals(g + 2, s.file.generation());
    }

    @Test
    void namesThatClashAreRefused() {
        ConfigFile.Builder b = ConfigFile.builder();
        b.bool("minimap", true);
        assertThrows(IllegalArgumentException.class, () -> b.section("minimap", s -> s.bool("enabled", true)));
        assertThrows(IllegalArgumentException.class, () -> b.bool("a.b", true));
        assertThrows(IllegalArgumentException.class, () -> b.integer("r", 5, 10, 20));
        assertThrows(IllegalStateException.class, () -> ConfigFile.builder().push("x").build());
    }

    /** A settings file of Teyvat Cartographia (0.8) reads without loss and is written back as it was. */
    @Test
    void theSettingsFileOfTeyvatCartographiaCarriesOver(@TempDir Path dir) throws IOException {
        Path json = dir.resolve("teyvatcartographia-client.json");
        String original = Files.readString(Path.of(System.getProperty("aacs.projectDir"), "src", "test", "resources",
                "teyvatcartographia-client.json"), StandardCharsets.UTF_8);
        Files.writeString(json, original, StandardCharsets.UTF_8);

        ConfigFile.Builder b = ConfigFile.builder();
        b.push("appearance");
        StringValue theme = b.text("theme", "default", t -> t.matches("[a-z0-9_.-]+"));
        b.pop().push("minimap");
        BoolValue enabled = b.bool("enabled", true);
        IntValue radius = b.integer("radius", 48, 24, 110);
        EnumValue<Corner> corner = b.choice("corner", Corner.TOP_LEFT);
        DoubleValue opacity = b.decimal("opacity", 0.95, 0.1, 1.0);
        b.pop().push("cartography");
        EnumValue<Detail> detail = b.choice("detail", Detail.HIGH);
        ListValue hidden = b.list("plantsHidden", List.of(), id -> id.contains(":"));
        b.pop().push("compat").push("xaero");
        BoolValue deaths = b.bool("deaths", true);
        b.pop().pop();
        ConfigFile file = b.build();
        assertTrue(file.load(json));

        assertEquals("default", theme.get());
        assertTrue(enabled.get());
        assertEquals(48, radius.get());
        assertEquals(Corner.TOP_LEFT, corner.get());
        assertEquals(0.95, opacity.get());
        assertEquals(Detail.HIGH, detail.get());
        assertEquals(List.of(), hidden.get());
        assertFalse(deaths.get(), "the file's value, not the default");
        assertEquals(JsonParser.parseString(original), JsonParser.parseString(Files.readString(json)),
                "every key the spec does not know kept, the known ones unchanged");
    }
}
