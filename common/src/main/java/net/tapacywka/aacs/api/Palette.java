package net.tapacywka.aacs.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The colours of a settings screen, by name (ARGB). Every colour may be set; the ones a mod leaves alone keep the
 * defaults, the colours of Teyvat Cartographia's settings screen.
 */
public final class Palette {
    /** The names a palette knows, with their defaults. */
    public static final Map<String, Integer> DEFAULTS;

    static {
        Map<String, Integer> d = new LinkedHashMap<>();
        d.put("settings_dim", 0x60000000);
        d.put("panel_dark", 0xB8272F3A);
        d.put("panel_dark_header", 0xC8383F4C);
        d.put("panel_header_line", 0x30000000);
        d.put("panel_dark_tile", 0xFF2E3747);
        d.put("panel_dark_tile_hover", 0xFF3A4558);
        d.put("panel_line", 0x40FFFFFF);
        d.put("info_box", 0xB3232833);
        d.put("gold", 0xFFD3BC8E);
        d.put("cream", 0xFFECE5D8);
        d.put("white", 0xFFFFFFFF);
        d.put("yellow", 0xFFFFCC33);
        d.put("text_muted", 0xFF9AA3B2);
        d.put("button", 0xFF4A5467);
        d.put("button_hover", 0xFF5A6680);
        d.put("button_hover_outline", 0x80FFFFFF);
        d.put("button_disabled", 0xFF7A808C);
        d.put("button_icon_badge", 0xFF363531);
        d.put("button_icon_disabled", 0xFFB0B0B0);
        d.put("button_text_disabled", 0xFFDDDDDD);
        d.put("scroll_track", 0x40FFFFFF);
        d.put("scroll_thumb", 0xFFECE5D8);
        d.put("toggle_track_off", 0xFF2B3340);
        d.put("toggle_track_on", 0xFFD3BC8E);
        d.put("toggle_knob", 0xFFECE5D8);
        d.put("toggle_outline", 0xC0ECE5D8);
        d.put("toggle_outline_hover", 0xFFFFFFFF);
        d.put("toggle_off_cross", 0xFFB79A62);
        d.put("slider_tick", 0xFF8D7A55);
        d.put("cursor", 0xFFECE5D8);
        d.put("cursor_outline", 0xFF1B2029);
        DEFAULTS = Collections.unmodifiableMap(d);
    }

    public static final Palette DEFAULT = new Palette();

    private final Map<String, Integer> colors = new LinkedHashMap<>(DEFAULTS);

    /** A palette of the default colours, to change with {@link #with}. */
    public Palette() {}

    /** This palette with colour {@code name} set (a name {@link #DEFAULTS} does not know throws). */
    public Palette with(String name, int argb) {
        if (!DEFAULTS.containsKey(name)) throw new IllegalArgumentException("no colour named " + name + "; known: " + DEFAULTS.keySet());
        Palette p = new Palette();
        p.colors.putAll(colors);
        p.colors.put(name, argb);
        return p;
    }

    public int get(String name) {
        Integer c = colors.get(name);
        if (c == null) throw new IllegalArgumentException("no colour named " + name);
        return c;
    }
}
