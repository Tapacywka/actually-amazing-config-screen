package net.tapacywka.aacs.impl.render;

import net.minecraft.resources.ResourceLocation;
import net.tapacywka.aacs.api.Aacs;
import net.tapacywka.aacs.api.Palette;

/**
 * The colours and shapes the screen is drawn with now, as fields under the names of Teyvat Cartographia's theme (the
 * drawing code came from there): {@link #use} fills them from the palette of the screen that opens.
 */
public final class Theme {
    private Theme() {}

    public static int SETTINGS_DIM, PANEL_DARK, PANEL_DARK_HEADER, PANEL_HEADER_LINE, PANEL_DARK_TILE, PANEL_DARK_TILE_HOVER, PANEL_LINE,
            INFO_BOX, GOLD, CREAM, WHITE, YELLOW, TEXT_MUTED, BUTTON, BUTTON_HOVER, BUTTON_HOVER_OUTLINE, BUTTON_DISABLED,
            BUTTON_ICON_BADGE, BUTTON_ICON_DISABLED, BUTTON_TEXT_DISABLED, SCROLL_TRACK, SCROLL_THUMB, TOGGLE_TRACK_OFF,
            TOGGLE_TRACK_ON, TOGGLE_KNOB, TOGGLE_OUTLINE, TOGGLE_OUTLINE_HOVER, TOGGLE_OFF_CROSS, SLIDER_TICK, TOOLTIP, TOOLTIP_OUTLINE, CURSOR,
            CURSOR_OUTLINE;

    private static Palette shown;
    private static int generation;

    static {
        use(Palette.DEFAULT, true);
    }

    /** The shapes: the rectangular interface (true) or the rounded one. */
    public static final class Style {
        private Style() {}

        public static boolean squareUi = true;
    }

    /** Draws with {@code p} and the given shapes from now on; a change bumps {@link #generation()}. */
    public static void use(Palette p, boolean square) {
        if (p == shown && square == Style.squareUi) return;
        shown = p;
        Style.squareUi = square;
        generation++;
        SETTINGS_DIM = p.get("settings_dim");
        PANEL_DARK = p.get("panel_dark");
        PANEL_DARK_HEADER = p.get("panel_dark_header");
        PANEL_HEADER_LINE = p.get("panel_header_line");
        PANEL_DARK_TILE = p.get("panel_dark_tile");
        PANEL_DARK_TILE_HOVER = p.get("panel_dark_tile_hover");
        PANEL_LINE = p.get("panel_line");
        INFO_BOX = p.get("info_box");
        GOLD = p.get("gold");
        CREAM = p.get("cream");
        WHITE = p.get("white");
        YELLOW = p.get("yellow");
        TEXT_MUTED = p.get("text_muted");
        BUTTON = p.get("button");
        BUTTON_HOVER = p.get("button_hover");
        BUTTON_HOVER_OUTLINE = p.get("button_hover_outline");
        BUTTON_DISABLED = p.get("button_disabled");
        BUTTON_ICON_BADGE = p.get("button_icon_badge");
        BUTTON_ICON_DISABLED = p.get("button_icon_disabled");
        BUTTON_TEXT_DISABLED = p.get("button_text_disabled");
        SCROLL_TRACK = p.get("scroll_track");
        TOOLTIP = p.get("tooltip");
        TOOLTIP_OUTLINE = p.get("tooltip_outline");
        SCROLL_THUMB = p.get("scroll_thumb");
        TOGGLE_TRACK_OFF = p.get("toggle_track_off");
        TOGGLE_TRACK_ON = p.get("toggle_track_on");
        TOGGLE_KNOB = p.get("toggle_knob");
        TOGGLE_OUTLINE = p.get("toggle_outline");
        TOGGLE_OUTLINE_HOVER = p.get("toggle_outline_hover");
        TOGGLE_OFF_CROSS = p.get("toggle_off_cross");
        SLIDER_TICK = p.get("slider_tick");
        CURSOR = p.get("cursor");
        CURSOR_OUTLINE = p.get("cursor_outline");
    }

    /** Goes up whenever the colours or shapes change: what was made from them (the pointers) is made again. */
    public static int generation() {
        return generation;
    }

    /** {@code a} to {@code b} by {@code t}, each channel. */
    public static int lerp(int a, int b, float t) {
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int ca = (a >>> s) & 0xFF, cb = (b >>> s) & 0xFF;
            r |= (Math.round(ca + (cb - ca) * t) & 0xFF) << s;
        }
        return r;
    }

    /** A file of the library's own assets ({@code assets/aacs/<path>}). */
    public static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath(Aacs.MOD_ID, path);
    }
}
