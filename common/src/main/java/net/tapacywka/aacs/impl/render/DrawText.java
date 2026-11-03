package net.tapacywka.aacs.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Text in GUI coordinates: always the vanilla Minecraft font at its native 1x size, snapped to the physical pixel grid
 * (fractional scales distort pixel fonts).
 */
public final class DrawText {
    private DrawText() {}

    /** Height of a line of text in GUI units. Glyphs occupy the top 8 units; centre text on a box with {@link #textY}. */
    public static final int LINE = 9;

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static int width(String s) {
        return font().width(s);
    }

    public static int width(Component c) {
        return font().width(c);
    }

    /** Y for text vertically centred in a box [top, top + height). */
    public static float textY(float top, float height) {
        return top + (height - 8f) / 2f;
    }

    public static void text(GuiGraphics g, Component text, float x, float y, int color, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(Draw.snap(x), Draw.snap(y), 0);
        g.drawString(font(), text, 0, 0, color, shadow);
        g.pose().popPose();
    }

    public static void text(GuiGraphics g, String text, float x, float y, int color, boolean shadow) {
        text(g, Component.literal(text), x, y, color, shadow);
    }

    /**
     * Width of the visible glyphs. {@link Font#width} includes the 1-unit advance after the last character,
     * which would push centred text half a unit left and right-aligned text one unit short of the edge.
     */
    private static int visualWidth(Component text) {
        return Math.max(0, width(text) - 1);
    }

    public static void textCentered(GuiGraphics g, Component text, float cx, float y, int color, boolean shadow) {
        text(g, text, cx - visualWidth(text) / 2f, y, color, shadow);
    }

    public static void textRight(GuiGraphics g, Component text, float right, float y, int color, boolean shadow) {
        text(g, text, right - visualWidth(text), y, color, shadow);
    }

    /**
     * Development check of the room texts get (devtest {@code FitAuditTest}): null in play, so nothing is measured. The
     * probe learns the place from the call stack.
     */
    public static TextRoom probe;

    /** Receives a text, its width and the room it has (all in GUI units). */
    public interface TextRoom {
        void report(String text, float width, float room);
    }

    /** Reports a text drawn without shortening and the room it has, while the probe is on. */
    public static void room(Component text, float room) {
        if (probe != null) probe.report(text.getString(), width(text), room);
    }

    public static void room(String text, float room) {
        if (probe != null) probe.report(text, width(text), room);
    }

    /** Shortens a string with an ellipsis to fit {@code maxWidth}. */
    public static String fit(String s, int maxWidth) {
        if (probe != null) probe.report(s, width(s), maxWidth);
        if (width(s) <= maxWidth) return s;
        String e = "…";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (width(s.substring(0, mid) + e) <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + e;
    }
}
