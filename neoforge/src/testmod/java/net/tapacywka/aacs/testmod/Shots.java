package net.tapacywka.aacs.testmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.tapacywka.aacs.api.AacsScreen;

/**
 * Development only ({@code -Daacs.shots=true}): opens the settings screen over the title screen, saves a shot of
 * each step to {@code screenshots/} and closes the game.
 */
final class Shots {
    private Shots() {}

    private static int tick = -1, step;

    static void start(AacsScreen screen) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (tick < 0) {
                if (!(mc.screen instanceof TitleScreen)) return;
                mc.setScreen(screen.open(mc.screen));
                tick = 0;
            }
            if (++tick % 40 != 0) return;
            switch (step++) {
                case 0 -> shot(mc, "aacs_tab1");
                case 1 -> {
                    // The sub-section opened (its row is the fourth: section, two switches, then it).
                    click(mc, 4);
                }
                case 2 -> shot(mc, "aacs_tab1_open");
                case 3 -> mc.screen.mouseScrolled(0, 0, 0, -3);
                case 4 -> shot(mc, "aacs_tab1_scrolled");
                default -> mc.stop();
            }
        });
    }

    /** A click on the middle of row {@code n} (1 first) of the list, as the screen lays it out at the top. */
    private static void click(Minecraft mc, int n) {
        var s = mc.screen;
        float listTop = 8 + 28 + 4 + 20 + 4; // margin, header, gap, tab strip, gap
        float y = listTop + 24 + (n - 2) * 20 + 10; // the section is 24 high, the rows 20
        s.mouseClicked(s.width / 2.0, y, 0);
        s.mouseReleased(s.width / 2.0, y, 0);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }
}
