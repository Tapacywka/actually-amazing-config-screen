package net.tapacywka.aacs;

import net.minecraft.client.gui.GuiGraphics;
import net.tapacywka.aacs.api.AacsScreen;
import net.tapacywka.aacs.impl.gui.ConfigScreen;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** When the settings screen goes, each picture is told once, though a tab's picture serves many rows. */
class PreviewClosedTest {
    private static final class Counting implements AacsScreen.Preview {
        int closed;

        @Override
        public void draw(GuiGraphics g, String row, float x, float y, float w, float h, float partialTick) {}

        @Override
        public void closed() {
            closed++;
        }
    }

    @Test
    void eachPictureIsToldOnce() {
        Counting tab = new Counting(), own = new Counting();
        // Rows of a tab sharing its picture, a row with its own, rows with none.
        ConfigScreen.closePreviews(Arrays.asList(tab, tab, null, own, tab, null));
        assertEquals(1, tab.closed);
        assertEquals(1, own.closed);
    }

    @Test
    void aPictureThatDoesNotListenIsLeftAlone() {
        AacsScreen.Preview quiet = (g, row, x, y, w, h, pt) -> {};
        ConfigScreen.closePreviews(Arrays.asList(quiet, AacsScreen.Preview.NONE));
    }
}
