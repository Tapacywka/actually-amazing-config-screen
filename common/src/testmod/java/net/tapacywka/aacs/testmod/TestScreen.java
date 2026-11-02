package net.tapacywka.aacs.testmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.tapacywka.aacs.api.AacsScreen;
import net.tapacywka.aacs.api.BoolValue;
import net.tapacywka.aacs.api.ConfigFile;
import net.tapacywka.aacs.api.DoubleValue;
import net.tapacywka.aacs.api.EnumValue;
import net.tapacywka.aacs.api.IntValue;
import net.tapacywka.aacs.impl.gui.ConfigScreen;

import java.nio.file.Path;

/**
 * Development only, shared by the test mods of both loaders: the settings, the screen with every kind of row, and
 * the shots ({@code -Daacs.shots=true}: the screen over the title screen, a shot of each step to {@code screenshots/},
 * then the game closes; each loader calls {@link #tick} every client tick).
 */
public final class TestScreen {
    private TestScreen() {}

    enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    static BoolValue GRID, LABELS, A, B, MAP, MINIMAP, FLOWERS;
    static net.tapacywka.aacs.api.ListValue FLOWERS_OFF;
    static IntValue OPACITY;
    static DoubleValue ZOOM;
    static EnumValue<Corner> CORNER;
    static final BoolValue[] MORE = new BoolValue[16];

    public static AacsScreen make(Path configDir) {
        ConfigFile.Builder b = ConfigFile.builder();
        b.section("look", s -> {
            GRID = s.bool("grid", true);
            LABELS = s.bool("labels", false);
            OPACITY = s.integer("opacity", 80, 0, 100);
            ZOOM = s.decimal("zoom", 1.0, 0.25, 4.0);
            CORNER = s.choice("corner", Corner.TOP_LEFT);
            MAP = s.bool("map", true);
            MINIMAP = s.bool("minimap", false);
            FLOWERS = s.bool("flowers", true);
            FLOWERS_OFF = s.list("flowersOff", java.util.List.of("poppy"), id -> !id.isEmpty());
        });
        b.section("details", s -> {
            A = s.bool("a", true);
            B = s.bool("b", false);
        });
        b.push("more");
        for (int i = 0; i < MORE.length; i++) MORE[i] = b.bool("r" + i, i % 3 == 0);
        b.pop();
        ConfigFile file = b.build();
        file.load(configDir.resolve("aacs_testmod-client.json"));

        String k = "aacs_testmod.config.";
        return AacsScreen.builder("aacs_testmod", file)
                .rounded(() -> rounded)
                .tab(k + "tab.display", t -> {
                    t.section(k + "section.look").toggle(GRID).toggle(LABELS)
                            .slider(OPACITY, s -> s.unit("%"))
                            .preview((g, x, y, w, h, pt) -> {
                                // A gold square as opaque as the setting, over a checked ground.
                                float side = Math.min(w, h) * 0.6f, sx = x + (w - side) / 2, sy = y + (h - side) / 2;
                                for (int i = 0; i < 8; i++) for (int j = 0; j < 8; j++) {
                                    int c = (i + j) % 2 == 0 ? 0xFF555555 : 0xFF888888;
                                    g.fill((int) (sx + i * side / 8), (int) (sy + j * side / 8), (int) (sx + (i + 1) * side / 8), (int) (sy + (j + 1) * side / 8), c);
                                }
                                int a = Math.round(OPACITY.get() * 2.55f);
                                g.fill((int) sx, (int) sy, (int) (sx + side), (int) (sy + side), a << 24 | 0xD3BC8E);
                            })
                            .slider(ZOOM, s -> s.steps(0.25, 0.5, 1, 2, 4))
                            .choice(CORNER)
                            .steps(k + "look.show", new String[]{k + "step.off", k + "step.map", k + "step.both"},
                                    () -> MAP.get() ? MINIMAP.get() ? 2 : 1 : 0, step -> { MAP.set(step >= 1); MINIMAP.set(step >= 2); })
                            .toggleList(FLOWERS, FLOWERS_OFF, TestScreen::flowers, null);
                    t.subSection(k + "sub.details").toggle(A).toggle(B);
                })
                .tab(k + "tab.extra", t -> {
                    t.section(k + "section.more");
                    for (BoolValue v : MORE) t.toggle(k + "more.n", v);
                })
                .build();
    }

    /** The entries of the test list: a few flowers with their colours. */
    private static java.util.List<AacsScreen.ListEntry> flowers() {
        String[] ids = {"poppy", "dandelion", "cornflower", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip",
                "pink_tulip", "oxeye_daisy", "lily_of_the_valley", "blue_orchid", "sunflower", "lilac", "peony"};
        int[] colours = {0xFFC0262A, 0xFFF2D23A, 0xFF4F6FD8, 0xFFB57AE0, 0xFFD8E2E8, 0xFFD43A2E, 0xFFE58A2E, 0xFFEDEDED,
                0xFFF0A8C8, 0xFFF4F4EA, 0xFFF8F8F8, 0xFF2EA8E8, 0xFFF2C21A, 0xFFC890D8, 0xFFE8A0C0};
        java.util.List<AacsScreen.ListEntry> l = new java.util.ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            l.add(new AacsScreen.ListEntry(ids[i], net.minecraft.network.chat.Component.translatable("block.minecraft." + ids[i]), colours[i]));
        }
        return l;
    }

    // ------------------------------------------------------------------ shots

    /** The rounded style, switched on for the last shot. */
    static boolean rounded;

    private static int tick = -1, step;

    public static void tick(AacsScreen screen, String prefix) {
        Minecraft mc = Minecraft.getInstance();
        if (tick < 0) {
            if (!(mc.screen instanceof TitleScreen)) return;
            mc.setScreen(screen.open(mc.screen));
            tick = 0;
        }
        if (++tick % 40 != 0) return;
        switch (step++) {
            case 0 -> {
                // Values changed and the pointer on the Opacity row (the fourth): its reset icon shows.
                OPACITY.set(40);
                ConfigScreen.testPointer = at(mc, 4);
            }
            case 1 -> shot(mc, prefix + "_reset");
            case 2 -> {
                ConfigScreen.testPointer = null;
                click(mc, 9); // the sub-section: the section, eight rows, then it
            }
            case 3 -> shot(mc, prefix + "_open");
            case 4 -> {
                // The list of the Flowers row (the eighth) opened by its arrow.
                int[] p = at(mc, 8);
                mc.screen.mouseClicked(contentRight(mc.screen.width) - 28 - 8 - 3, p[1], 0); // the arrow left of the switch
            }
            case 5 -> shot(mc, prefix + "_list");
            case 6 -> {
                rounded = true;
                ConfigScreen.testPointer = at(mc, 4);
            }
            case 7 -> shot(mc, prefix + "_rounded");
            case 8 -> {
                // A window too narrow for the card: the description shows as a tooltip by the row.
                rounded = false;
                // 560 GUI units: the panel and its margins, too little left for the card (200) at any scale.
                int w = (int) (mc.getWindow().getGuiScale() * 560);
                org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), w, mc.getWindow().getHeight());
            }
            case 9 -> ConfigScreen.testPointer = at(mc, 4);
            case 10 -> shot(mc, prefix + "_narrow");
            default -> {
                OPACITY.reset();
                mc.stop();
            }
        }
    }

    /** The middle of row {@code n} (1 first; the section is row 1) in GUI units, the pointer over the controls. */
    private static int[] at(Minecraft mc, int n) {
        var s = mc.screen;
        return new int[]{(int) contentRight(s.width) - 60, 8 + 28 + 4 + 20 + 4 + 24 + (n - 2) * 20 + 10};
    }

    /** The right edge of the rows, as the screen lays out its panel and the card beside it. */
    private static float contentRight(int width) {
        float panelW = Math.min(360, (width - 32) / 4 * 4), rest = width - 16 - panelW - 8;
        float cardW = rest >= 200 ? Math.min(300, rest) : 0;
        float panelX = Math.round((width - panelW - (cardW > 0 ? 8 + cardW : 0)) / 2f);
        return panelX + panelW - 16;
    }

    private static void click(Minecraft mc, int n) {
        int[] p = at(mc, n);
        mc.screen.mouseClicked(p[0], p[1], 0);
        mc.screen.mouseReleased(p[0], p[1], 0);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }
}
