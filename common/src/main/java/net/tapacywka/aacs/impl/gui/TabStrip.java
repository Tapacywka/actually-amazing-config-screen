package net.tapacywka.aacs.impl.gui;

/**
 * The tabs of the settings screen when they do not fit in the panel (user request 08.11.2026, variant A of three
 * mockups): each tab as wide as its title needs, in a strip that scrolls sideways between two arrow buttons. Pure: where
 * the strip goes for an arrow or a wheel notch, and for a tab clicked half hidden. Positions are how far the strip is
 * scrolled, in GUI units, 0 with the first tab at the left edge of the view.
 */
public final class TabStrip {
    private TabStrip() {}

    /** Closer than this counts as the same place (the eased position never lands exactly). */
    private static final float SAME = 0.5f;

    /** Where each tab starts in the strip, scrolled 0. */
    public static float[] starts(float[] widths, float gap) {
        float[] out = new float[widths.length];
        float x = 0;
        for (int i = 0; i < widths.length; i++) {
            out[i] = x;
            x += widths[i] + gap;
        }
        return out;
    }

    /** The whole strip, the gaps between the tabs too. */
    public static float length(float[] widths, float gap) {
        float x = 0;
        for (float w : widths) x += w;
        return x + Math.max(0, widths.length - 1) * gap;
    }

    /** The furthest the strip scrolls in a view {@code view} wide (0 when it fits). */
    public static float max(float[] widths, float gap, float view) {
        return Math.max(0, length(widths, gap) - view);
    }

    /** One step on from {@code at}: the next tab's start at the left edge, at most {@code max} (the last tab at the right edge). */
    public static float next(float at, float[] starts, float max) {
        for (float s : starts) if (s > at + SAME) return Math.min(s, max);
        return max;
    }

    /** One step back from {@code at}: the start of the tab before the one at the left edge, at least 0. */
    public static float previous(float at, float[] starts, float max) {
        float from = Math.min(at, max), out = 0;
        for (float s : starts) if (s < from - SAME) out = s;
        return out;
    }

    /** Can the strip go on from {@code at} towards its end, or back towards its start. */
    public static boolean canNext(float at, float max) {
        return at < max - SAME;
    }

    public static boolean canPrevious(float at) {
        return at > SAME;
    }

    /** Scrolled from {@code at} as little as it takes to show the tab at {@code start}, {@code width} wide, whole. */
    public static float reveal(float at, float start, float width, float view, float max) {
        float to = at;
        if (start + width > to + view) to = start + width - view;
        if (start < to) to = start;
        return Math.max(0, Math.min(max, to));
    }
}
