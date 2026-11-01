package net.tapacywka.aacs.testmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.tapacywka.aacs.api.AacsScreen;
import net.tapacywka.aacs.api.BoolValue;
import net.tapacywka.aacs.api.ConfigFile;

/**
 * Development only: the same settings screen as the NeoForge test mod, opened from Mod Menu; with
 * {@code -Daacs.shots=true} it opens over the title screen, saves shots to {@code screenshots/} and closes the game.
 */
public final class AacsTestModFabric implements ClientModInitializer, ModMenuApi {
    private static AacsScreen screen;
    private static int tick = -1, step;

    @Override
    public void onInitializeClient() {
        ConfigFile.Builder b = ConfigFile.builder();
        BoolValue[] look = new BoolValue[2], details = new BoolValue[2], more = new BoolValue[16];
        b.section("look", s -> {
            look[0] = s.bool("grid", true);
            look[1] = s.bool("labels", false);
        });
        b.section("details", s -> {
            details[0] = s.bool("a", true);
            details[1] = s.bool("b", false);
        });
        b.push("more");
        for (int i = 0; i < more.length; i++) more[i] = b.bool("r" + i, i % 3 == 0);
        b.pop();
        ConfigFile file = b.build();
        file.load(FabricLoader.getInstance().getConfigDir().resolve("aacs_testmod-client.json"));
        String k = "aacs_testmod.config.";
        screen = AacsScreen.builder("aacs_testmod", file)
                .tab(k + "tab.display", t -> {
                    t.section(k + "section.look").toggle(look[0]).toggle(look[1]);
                    t.subSection(k + "sub.details").toggle(details[0]).toggle(details[1]);
                })
                .tab(k + "tab.extra", t -> {
                    t.section(k + "section.more");
                    for (BoolValue v : more) t.toggle(k + "more.n", v);
                })
                .build();
        if (Boolean.getBoolean("aacs.shots")) ClientTickEvents.END_CLIENT_TICK.register(AacsTestModFabric::shots);
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> screen.open(parent);
    }

    private static void shots(Minecraft mc) {
        if (tick < 0) {
            if (!(mc.screen instanceof TitleScreen)) return;
            mc.setScreen(screen.open(mc.screen));
            tick = 0;
        }
        if (++tick % 40 != 0) return;
        switch (step++) {
            case 0 -> shot(mc, "aacs_fabric_tab1");
            case 1 -> {
                var s = mc.screen;
                float y = 8 + 28 + 4 + 20 + 4 + 24 + 2 * 20 + 10; // the sub-section's row: after the section and two switches
                s.mouseClicked(s.width / 2.0, y, 0);
                s.mouseReleased(s.width / 2.0, y, 0);
            }
            case 2 -> shot(mc, "aacs_fabric_tab1_open");
            default -> mc.stop();
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }
}
