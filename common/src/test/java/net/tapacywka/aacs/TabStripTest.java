package net.tapacywka.aacs;

import net.tapacywka.aacs.impl.gui.TabStrip;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tabs that do not fit scroll sideways between two arrows: a tab a click, never past either end. */
class TabStripTest {
    /** Six tabs as wide as Teyvat Cartographia's in Russian, 4 apart, in a view of 280. */
    private static final float[] W = {46, 72, 46, 58, 94, 88};
    private static final float GAP = 4, VIEW = 280;

    @Test
    void theStripIsItsTabsAndGaps() {
        assertArrayEquals(new float[] {0, 50, 126, 176, 238, 336}, TabStrip.starts(W, GAP));
        assertEquals(424, TabStrip.length(W, GAP));
        assertEquals(144, TabStrip.max(W, GAP, VIEW));
        assertEquals(0, TabStrip.max(W, GAP, 500), "tabs that fit do not scroll");
    }

    @Test
    void theArrowsGoATabAtATimeAndStopAtTheEnds() {
        float[] s = TabStrip.starts(W, GAP);
        float max = TabStrip.max(W, GAP, VIEW);
        float at = 0;
        at = TabStrip.next(at, s, max);
        assertEquals(50, at, "the second tab at the left edge");
        at = TabStrip.next(at, s, max);
        assertEquals(126, at);
        at = TabStrip.next(at, s, max);
        assertEquals(144, at, "the last tab at the right edge, not a start past it");
        assertFalse(TabStrip.canNext(at, max));
        assertEquals(144, TabStrip.next(at, s, max), "nothing past the end");
        at = TabStrip.previous(at, s, max);
        assertEquals(126, at, "back from the end: the tab before the one cut at the left edge");
        at = TabStrip.previous(at, s, max);
        assertEquals(50, at);
        at = TabStrip.previous(at, s, max);
        assertEquals(0, at);
        assertFalse(TabStrip.canPrevious(at));
        assertEquals(0, TabStrip.previous(at, s, max), "nothing before the start");
        assertTrue(TabStrip.canNext(0, max));
        assertTrue(TabStrip.canPrevious(max));
    }

    @Test
    void anEasedPositionCountsAsTheTabItIsAt() {
        float[] s = TabStrip.starts(W, GAP);
        float max = TabStrip.max(W, GAP, VIEW);
        assertEquals(126, TabStrip.next(49.8f, s, max), "at the second tab already: on to the third");
        assertEquals(0, TabStrip.previous(50.3f, s, max));
    }

    @Test
    void aTabClickedHalfHiddenComesWhole() {
        float[] s = TabStrip.starts(W, GAP);
        float max = TabStrip.max(W, GAP, VIEW);
        assertEquals(0, TabStrip.reveal(0, s[1], W[1], VIEW, max), "a tab in view stays put");
        assertEquals(s[4] + W[4] - VIEW, TabStrip.reveal(0, s[4], W[4], VIEW, max), "cut on the right: its end at the right edge");
        assertEquals(max, TabStrip.reveal(0, s[5], W[5], VIEW, max), "the last one: the end of the strip");
        assertEquals(s[1], TabStrip.reveal(max, s[1], W[1], VIEW, max), "cut on the left: its start at the left edge");
        assertEquals(0, TabStrip.reveal(max, s[0], W[0], VIEW, max));
    }
}
