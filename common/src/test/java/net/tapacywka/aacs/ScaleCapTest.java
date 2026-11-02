package net.tapacywka.aacs;

import net.tapacywka.aacs.impl.gui.ConfigScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The settings are never larger than GUI 3 at 1080p: the largest whole scale for a window by its height. */
class ScaleCapTest {
    @Test
    void theCapFollowsTheWindowHeight() {
        assertEquals(3, ConfigScreen.scaleCap(1080), "1080p: GUI 4 shows as 3");
        assertEquals(4, ConfigScreen.scaleCap(1440), "1440p: GUI 5 to 8 show as 4");
        assertEquals(6, ConfigScreen.scaleCap(2160), "2160p");
        assertEquals(2, ConfigScreen.scaleCap(768), "a laptop: 2");
        assertEquals(2, ConfigScreen.scaleCap(1055), "a 1080p window under a title bar: 2");
        assertEquals(1, ConfigScreen.scaleCap(240), "never under 1");
    }
}
