package net.tapacywka.aacs.impl.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.tapacywka.aacs.impl.render.Backdrop;
import net.tapacywka.aacs.impl.render.Draw;
import net.tapacywka.aacs.impl.render.DrawText;
import net.tapacywka.aacs.impl.render.Icon;
import net.tapacywka.aacs.impl.render.Theme;

/** The screen's controls, as Teyvat Cartographia draws them: the panel, the action buttons, the switch. */
public final class Ui {
    private Ui() {}

    /** The layout grid, GUI units. */
    public static final class Grid {
        private Grid() {}

        public static final int STEP = 4;
        public static final int MARGIN = 8;
        public static final int PAD = 8;
        public static final int GAP = 4;
        public static final int ICON = 12;
        public static final int ICON_L = 16;
        public static final int HEADER_H = 28;
        public static final int CHIP_H = 20;
        public static final int ROW = 20;
        public static final int ACTION_H = 24;
        public static final int TOGGLE_W = 28, TOGGLE_H = 16;
    }

    // ------------------------------------------------------------------ panel chrome

    /** The panel: a translucent body over the blurred backdrop, a header with icon, gold title and the close cross. */
    public static void panel(GuiGraphicsExtractor g, float x, float y, float w, float h, Component title, Icon titleIcon, int mx, int my) {
        Backdrop.draw(g, x, y, x + w, y + h);
        Draw.rect(g, x, y, x + w, y + h, Theme.PANEL_DARK);
        Draw.rect(g, x, y, x + w, y + Grid.HEADER_H, Theme.PANEL_DARK_HEADER);
        Draw.rect(g, x, y + Grid.HEADER_H - 1, x + w, y + Grid.HEADER_H, Theme.PANEL_HEADER_LINE);
        float cy = y + Grid.HEADER_H / 2f;
        float tx = x + Grid.PAD;
        if (titleIcon != null) {
            Draw.icon(g, titleIcon, x + Grid.PAD + Grid.ICON_L / 2f, cy, Grid.ICON_L, Theme.GOLD);
            tx = x + Grid.PAD + Grid.ICON_L + Grid.GAP + 2;
        }
        float closeCx = x + w - Grid.PAD - Grid.ICON / 2f;
        int maxW = (int) (closeCx - Grid.ICON / 2f - Grid.GAP - tx);
        DrawText.text(g, DrawText.fit(title.getString(), maxW), tx, DrawText.textY(y, Grid.HEADER_H), Theme.GOLD, false);
        boolean hover = closeHit(x, y, w, mx, my);
        if (hover) Cursors.want(Cursors.Shape.HAND);
        Draw.icon(g, Icon.CLOSE, closeCx, cy, Grid.ICON, hover ? Theme.WHITE : Theme.CREAM);
    }

    public static boolean closeHit(float x, float y, float w, double mx, double my) {
        return Draw.inRect(mx, my, x + w - Grid.HEADER_H, y, Grid.HEADER_H, Grid.HEADER_H);
    }

    // ------------------------------------------------------------------ buttons

    /**
     * An action button ({@link Grid#ACTION_H} high): a dark badge with the icon in the left end (none for a null icon),
     * the label centred between it and the right edge; a capsule in the rounded style, a rectangle in the other.
     */
    public static void actionButton(GuiGraphicsExtractor g, float x, float y, float w, Component label, Icon icon,
                                    boolean enabled, int mx, int my) {
        float h = Grid.ACTION_H;
        boolean hover = enabled && Draw.inRect(mx, my, x, y, w, h);
        if (hover) Cursors.want(Cursors.Shape.HAND);
        else if (Draw.inRect(mx, my, x, y, w, h)) Cursors.want(Cursors.Shape.UNAVAILABLE);
        int fill = !enabled ? Theme.BUTTON_DISABLED : hover ? Theme.BUTTON_HOVER : Theme.BUTTON;
        float cx = x + h / 2f, cy = y + h / 2f, r = h / 2f - 2;
        if (Theme.Style.squareUi) {
            Draw.rect(g, x, y, x + w, y + h, fill);
            if (hover) Draw.frame(g, x, y, w, h, 1, Theme.BUTTON_HOVER_OUTLINE);
            if (icon != null) Draw.rect(g, cx - r, cy - r, cx + r, cy + r, Theme.BUTTON_ICON_BADGE);
        } else {
            Draw.capsule(g, x, y, w, h, fill);
            if (hover) Draw.capsuleOutline(g, x, y, w, h, 1f, Theme.BUTTON_HOVER_OUTLINE);
            if (icon != null) Draw.circle(g, cx, cy, r, Theme.BUTTON_ICON_BADGE);
        }
        if (icon != null) Draw.icon(g, icon, cx, cy, Grid.ICON, enabled ? Theme.YELLOW : Theme.BUTTON_ICON_DISABLED);
        float labelLeft = icon != null ? cx + r : x, labelRight = x + w;
        Component shown = Component.literal(DrawText.fit(label.getString(), (int) (labelRight - labelLeft - 2 * Grid.GAP)));
        DrawText.textCentered(g, shown, (labelLeft + labelRight) / 2f, DrawText.textY(y, h), enabled ? Theme.CREAM : Theme.BUTTON_TEXT_DISABLED, false);
    }

    public static boolean actionHit(float x, float y, float w, double mx, double my) {
        return Draw.inRect(mx, my, x, y, w, Grid.ACTION_H);
    }

    // ------------------------------------------------------------------ scroll bar arrows

    /**
     * The arrows of a scroll bar whose middle is {@code cx}, in the room {@link ScrollArrows#ROOM} at both ends of
     * {@code top} to {@code bottom}: cream, gold pointed at or held, dim where the list cannot go on.
     */
    public static void scrollArrows(GuiGraphicsExtractor g, float cx, float top, float bottom, SmoothScroll s, ScrollArrows a,
                                    int mx, int my) {
        scrollArrow(g, -1, cx, top, bottom, s, a, mx, my);
        scrollArrow(g, 1, cx, top, bottom, s, a, mx, my);
    }

    private static void scrollArrow(GuiGraphicsExtractor g, int dir, float cx, float top, float bottom, SmoothScroll s, ScrollArrows a,
                                    int mx, int my) {
        boolean can = ScrollArrows.can(dir, s), hover = ScrollArrows.at(mx, my, cx, top, bottom) == dir;
        if (hover) Cursors.want(can ? Cursors.Shape.HAND : Cursors.Shape.UNAVAILABLE);
        int c = !can ? (Theme.SCROLL_THUMB & 0xFFFFFF) | 0x4D000000 : hover || a.held() == dir ? Theme.GOLD : Theme.SCROLL_THUMB;
        float cy = ScrollArrows.middle(dir, top, bottom), hw = ScrollArrows.HALF_W, hh = ScrollArrows.TRI_H / 2f;
        if (dir < 0) Draw.triangle(g, cx, cy - hh, cx + hw, cy + hh, cx - hw, cy + hh, c);
        else Draw.triangle(g, cx - hw, cy - hh, cx + hw, cy - hh, cx, cy + hh, c);
    }

    /**
     * A check box of {@code s} units (a row of {@code AacsScreen.Tab#checks}): an outline while not ticked, gold with a
     * dark tick while ticked; white under the pointer, darker while it cannot be clicked.
     */
    public static void checkBox(GuiGraphicsExtractor g, float x, float y, float s, boolean on, boolean active, boolean hover) {
        int edge = !active ? inactive(Theme.CREAM) : hover ? Theme.WHITE : Theme.CREAM;
        int fill = active ? Theme.GOLD : inactive(Theme.GOLD);
        if (Theme.Style.squareUi) {
            if (on) Draw.rect(g, x, y, x + s, y + s, fill);
            if (!on || hover && active) Draw.frame(g, x, y, s, s, 1, edge);
        } else {
            if (on) Draw.roundRect(g, x, y, s, s, 2, fill);
            if (!on || hover && active) Draw.roundRectOutline(g, x, y, s, s, 2, 1, edge);
        }
        if (on) Draw.icon(g, Icon.CHECK, x + s / 2f, y + s / 2f, s - 2, Theme.TOGGLE_TRACK_OFF);
    }

    /** How much darker an inactive control is. */
    public static final float INACTIVE = 0.4f;

    public static int inactive(int c) {
        return Theme.lerp(c, c & 0xFF000000, INACTIVE);
    }

    /** The switch ({@link Grid#TOGGLE_W} x {@link Grid#TOGGLE_H}). */
    public static void toggle(GuiGraphicsExtractor g, float x, float y, boolean on, int mx, int my) {
        float w = Grid.TOGGLE_W, h = Grid.TOGGLE_H, r = h / 2f;
        boolean hover = Draw.inRect(mx, my, x, y, w, h);
        if (hover) Cursors.want(Cursors.Shape.HAND);
        float kx = on ? x + w - r : x + r, ky = y + r;
        int track = on ? Theme.TOGGLE_TRACK_ON : Theme.TOGGLE_TRACK_OFF, outline = hover ? Theme.TOGGLE_OUTLINE_HOVER : Theme.TOGGLE_OUTLINE;
        if (Theme.Style.squareUi) {
            Draw.rect(g, x, y, x + w, y + h, track);
            Draw.frame(g, x, y, w, h, 1, outline);
            Draw.rect(g, kx - r + 3, ky - r + 3, kx + r - 3, ky + r - 3, Theme.TOGGLE_KNOB);
        } else {
            Draw.capsule(g, x, y, w, h, track);
            Draw.capsuleOutline(g, x, y, w, h, 1f, outline);
            Draw.circle(g, kx, ky, r - 3, Theme.TOGGLE_KNOB);
        }
        if (on) {
            if (Theme.Style.squareUi) Draw.rect(g, kx - 1, ky - 1, kx + 1, ky + 1, Theme.TOGGLE_TRACK_ON);
            else Draw.circle(g, kx, ky, 2f, Theme.TOGGLE_TRACK_ON);
        } else {
            Draw.icon(g, Icon.CLOSE, kx, ky, 6, Theme.TOGGLE_OFF_CROSS);
        }
    }

    public static boolean toggleHit(float x, float y, double mx, double my) {
        return Draw.inRect(mx, my, x - 2, y - 2, Grid.TOGGLE_W + 4, Grid.TOGGLE_H + 4);
    }

    /** The box of a tooltip: near opaque, a faint outline one unit inside in the rectangular style. */
    public static void tooltipBox(GuiGraphicsExtractor g, float x, float y, float w, float h) {
        if (Theme.Style.squareUi) {
            Draw.rect(g, x, y, x + w, y + h, Theme.TOOLTIP);
            Draw.frame(g, x + 1, y + 1, w - 2, h - 2, 1, Theme.TOOLTIP_OUTLINE);
        } else {
            Draw.roundRect(g, x, y, w, h, 3, Theme.TOOLTIP);
            Draw.roundRectOutline(g, x, y, w, h, 3, 1f, Theme.TOOLTIP_OUTLINE);
        }
    }
}
