package net.tapacywka.aacs.impl.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.tapacywka.aacs.impl.version.Game;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.tapacywka.aacs.api.AacsScreen;
import net.tapacywka.aacs.api.Value;
import net.tapacywka.aacs.impl.Extensions;
import net.tapacywka.aacs.impl.gui.Ui.Grid;
import net.tapacywka.aacs.impl.render.Backdrop;
import net.tapacywka.aacs.impl.render.Draw;
import net.tapacywka.aacs.impl.render.DrawText;
import net.tapacywka.aacs.impl.render.EdgeFade;
import net.tapacywka.aacs.impl.render.Icon;
import net.tapacywka.aacs.impl.render.Theme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The settings screen of an {@link AacsScreen}, as Teyvat Cartographia's: a panel with the tabs, the list of rows that
 * scrolls without steps and dissolves at its edges, its bar, and the buttons at the bottom (the mod's, Reset, Done).
 */
public final class ConfigScreen extends Screen {
    private static final int SECTION_H = 24;
    private static final int TAB_H = Grid.ROW;
    /** Height of the dissolving edges of the list, GUI units. */
    private static final float FADE = 12;
    private static final float BAR_W = 3;
    /** One wheel notch, GUI units: a row. */
    private static final float WHEEL = Grid.ROW + Grid.GAP;
    /** The panel's width at most. */
    private static final int PANEL_MAX = 360;

    /** Sub-sections the player opened, by mod and key (all start folded): kept while the game runs. */
    private static final Set<String> OPENED = new HashSet<>();
    /** The tab shown last, by mod: kept while the game runs. */
    private static final java.util.Map<String, Integer> SHOWN_TAB = new java.util.HashMap<>();

    private final AacsScreen spec;
    private final Screen parent;
    private final List<List<Row>> tabs = new ArrayList<>();
    private final Icon titleIcon;
    private final List<Icon> buttonIcons = new ArrayList<>();
    private int tab;
    private List<Row> rows;
    private final SmoothScroll scroller = new SmoothScroll();
    /** Without room for the card: the description of the row under the pointer. */
    private final ScrollTooltip tip = new ScrollTooltip();
    private float scroll;
    private boolean draggingBar;
    private Row draggingRow;
    private float barGrab;
    private float panelX, panelY, panelW, panelH, listTop, listBottom;
    private int blurW = -1, blurH = -1, blurTheme = -1, blurFrames;

    public ConfigScreen(AacsScreen spec, Screen parent) {
        super(spec.title);
        this.spec = spec;
        this.parent = parent;
        titleIcon = spec.icon == null ? Icon.GEAR : new Icon(spec.icon);
        for (AacsScreen.Button b : spec.buttons) buttonIcons.add(b.icon() == null ? null : new Icon(b.icon()));
        Extensions.checkAgainst(spec);
        for (AacsScreen.Tab t : spec.tabs) {
            List<Row> list = new ArrayList<>();
            addRows(list, t);
            // A tab open to other mods: their sub-sections after its own rows, by name (see AacsScreen.extend).
            if (t.openTo() != null) {
                List<AacsScreen.Tab> parts = new ArrayList<>();
                for (AacsScreen.Extension e : Extensions.of(spec.modId, t.openTo())) {
                    if (!e.shown()) continue;
                    parts.add(e.build());
                    extraFiles.add(e.file);
                }
                parts.sort(java.util.Comparator.comparing(ConfigScreen::partName, String.CASE_INSENSITIVE_ORDER));
                for (AacsScreen.Tab p : parts) {
                    int from = list.size();
                    addRows(list, p);
                    for (Row r : list.subList(from, list.size())) if (r.value != null) extraValues.add(r.value);
                }
            }
            List<String[]> steps = new ArrayList<>();
            for (Row r : list) if (r instanceof SliderRow sr && sr.s.stepKeys != null) steps.add(sr.s.stepKeys);
            for (Row r : list) if (r instanceof SliderRow sr && sr.s.stepKeys != null) sr.stepGroup = steps;
            tabs.add(list);
        }
        tab = Math.min(SHOWN_TAB.getOrDefault(spec.modId, 0), tabs.size() - 1);
        rows = tabs.get(tab);
    }

    /** The title of an added sub-section (its first row), as shown. */
    private static String partName(AacsScreen.Tab p) {
        AacsScreen.Row head = p.rows.get(0);
        return (head.label() != null ? head.label() : Component.translatable(head.key())).getString();
    }

    /** The files of the mods whose sub-sections this screen shows, written when it closes. */
    private final Set<net.tapacywka.aacs.api.ConfigFile> extraFiles = new java.util.LinkedHashSet<>();
    /** The values of those sub-sections: Reset sets them back too. */
    private final Set<Value<?>> extraValues = new java.util.LinkedHashSet<>();

    /** The rows of {@code t} after those of {@code list}; a row without a picture of its own takes {@code t}'s. */
    private void addRows(List<Row> list, AacsScreen.Tab t) {
        {
            SubSection owner = null;
            for (AacsScreen.Row r : t.rows) {
                Row row = switch (r.kind()) {
                    case SECTION -> new Section(r.key());
                    case SUB_SECTION -> new SubSection(spec.modId + "/" + r.key(), r.label() != null ? r.label() : Component.translatable(r.key()));
                    case TOGGLE -> new Toggle(r.key(), cast(r.get()), cast(r.set()));
                    case TOGGLE_LIST -> new ListRow(r.key(), cast(r.get()), cast(r.set()), cast(r.options()));
                    case SLIDER -> new SliderRow(r.key(), cast(r.options()), cast(r.get()), cast(r.set()));
                    case CHOICE -> new ChoiceRow<>(r.key(), cast(r.options()), cast(r.get()), cast(r.set()));
                    case ACTION -> new ActionRow(r.key(), (String) r.options(), cast(r.set()));
                };
                row.value = r.value();
                // The row's own picture, else its tab's; none (only the description) for NONE or without either.
                AacsScreen.Preview p = r.preview() != null ? r.preview() : t.preview();
                row.preview = p == AacsScreen.Preview.NONE ? null : p;
                row.langKey = r.key();
                row.then = r.then();
                if (row instanceof Section) owner = null;
                else if (row instanceof SubSection s) owner = s;
                else row.owner = owner;
                list.add(row);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    // ------------------------------------------------------------------ rows

    private abstract static class Row {
        final Component label;
        /** The longer description (the preview card, the tooltip); null for headings. */
        final Component tooltip;
        float y;
        /** The sub-heading the row belongs to: hidden (no height) while it is folded. */
        SubSection owner;

        Row(Component label, Component tooltip) {
            this.label = label;
            this.tooltip = tooltip;
        }

        boolean shown() {
            return owner == null || owner.open();
        }

        float listHeight() {
            return shown() ? height() : 0;
        }

        float height() {
            return Grid.ROW;
        }

        /** The setting behind the row (its reset icon), or null. */
        Value<?> value;
        /** The mod's picture of the setting on the card, or null. */
        AacsScreen.Preview preview;
        /** The row's lang key, as the picture is told which row it shows. */
        String langKey;
        /** Run after the player changed the row (see {@link AacsScreen.Tab#then}), or null. */
        Runnable then;

        void changed() {
            if (then != null) then.run();
        }

        /** The value as the row shows it ("On", "40%", "Top left"), for the card's caption; null for headings. */
        String valueText() {
            return null;
        }

        void renderLabel(GuiGraphicsExtractor g, float x, float w) {
            // Short of the middle by the reset icon too, so the label never jumps when the icon shows.
            DrawText.text(g, DrawText.fit(label.getString(), (int) (w / 2 - 2 * Grid.GAP - RESET)), x, DrawText.textY(y, Grid.ROW), Theme.WHITE, false);
        }

        abstract void render(GuiGraphicsExtractor g, float x, float w, int mx, int my);

        boolean click(double mx, double my, float x, float w) {
            return false;
        }

        void drag(double mx, float x, float w) {}

        void release() {}

        /** Where the row's control starts (the reset icon sits left of it). */
        float controlLeft(float x, float w) {
            return x + w / 2;
        }

        /** The reset icon's middle. */
        float resetX(float x, float w) {
            return controlLeft(x, w) - Grid.GAP - RESET / 2f;
        }

        /** The icon shows while the pointer is over a row whose setting differs from its default. */
        boolean resetShown(float x, float w, int mx, int my) {
            return value != null && !value.isDefault() && my >= y && my < y + Grid.ROW && mx >= x && mx < x + w;
        }

        void renderReset(GuiGraphicsExtractor g, float x, float w, int mx, int my) {
            if (!resetShown(x, w, mx, my)) return;
            float cx = resetX(x, w), cy = y + Grid.ROW / 2f;
            boolean hover = Draw.inRect(mx, my, cx - RESET / 2f - 1, cy - RESET / 2f - 1, RESET + 2, RESET + 2);
            if (hover) Cursors.want(Cursors.Shape.HAND);
            Draw.icon(g, Icon.RESET, cx, cy, RESET, hover ? Theme.GOLD : Theme.TEXT_MUTED);
        }

        boolean resetClick(double mx, double my, float x, float w) {
            if (!resetShown(x, w, (int) mx, (int) my)) return false;
            float cx = resetX(x, w), cy = y + Grid.ROW / 2f;
            if (!Draw.inRect(mx, my, cx - RESET / 2f - 1, cy - RESET / 2f - 1, RESET + 2, RESET + 2)) return false;
            value.reset();
            changed();
            return true;
        }
    }

    /** The reset icon of a row, GUI units. */
    private static final int RESET = 10;

    private static final class Section extends Row {
        Section(String key) {
            super(Component.translatable(key), null);
        }

        @Override
        float height() {
            return SECTION_H;
        }

        @Override
        void render(GuiGraphicsExtractor g, float x, float w, int mx, int my) {
            DrawText.text(g, DrawText.fit(label.getString(), (int) w), x, y + Grid.PAD, Theme.GOLD, false);
            Draw.rect(g, x, y + SECTION_H - 3, x + w, y + SECTION_H - 2, Theme.PANEL_LINE);
        }
    }

    /**
     * A sub-heading: a dark band with a gold mark on the left, folded at first; a click folds or unfolds its rows, the
     * small arrow on the right points down while open, right while folded.
     */
    private static final class SubSection extends Row {
        final String key;

        SubSection(String key, Component label) {
            super(label, null);
            this.key = key;
        }

        boolean open() {
            return OPENED.contains(key);
        }

        @Override
        void render(GuiGraphicsExtractor g, float x, float w, int mx, int my) {
            boolean hover = Draw.inRect(mx, my, x, y + 2, w, Grid.ROW - 4);
            if (hover) Cursors.want(Cursors.Shape.HAND);
            Draw.rect(g, x, y + 2, x + w, y + Grid.ROW - 2, hover ? Theme.PANEL_DARK_TILE_HOVER : Theme.INFO_BOX);
            Draw.rect(g, x, y + 2, x + 2, y + Grid.ROW - 2, Theme.GOLD);
            DrawText.text(g, DrawText.fit(label.getString(), (int) (w - 2 - 3 * Grid.GAP - 6)), x + 2 + Grid.GAP, DrawText.textY(y, Grid.ROW), Theme.CREAM, false);
            float ax = x + w - Grid.GAP - 3, ay = y + Grid.ROW / 2f;
            if (open()) Draw.triangle(g, ax - 3, ay - 1.5f, ax + 3, ay - 1.5f, ax, ay + 2f, Theme.CREAM);
            else Draw.triangle(g, ax - 1.5f, ay - 3, ax - 1.5f, ay + 3, ax + 2f, ay, Theme.CREAM);
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            if (!Draw.inRect(mx, my, x, y + 2, w, Grid.ROW - 4)) return false;
            if (!OPENED.remove(key)) OPENED.add(key);
            return true;
        }
    }

    /** The list open under its row, and that row; null while none is open. */
    private ToggleList openList;
    private ListRow openRow;

    private void closeList() {
        if (openList != null) openList.closed();
        openList = null;
        openRow = null;
    }

    /**
     * A switch with an arrow left of it that opens its list under the row (only while the switch is on; grey and still
     * while it is off).
     */
    private final class ListRow extends Toggle {
        final AacsScreen.ToggleList spec;

        ListRow(String key, Supplier<Boolean> get, Consumer<Boolean> set, AacsScreen.ToggleList spec) {
            super(key, get, set);
            this.spec = spec;
        }

        private float ax(float x, float w) {
            return x + w - Grid.TOGGLE_W - Grid.PAD - 3;
        }

        @Override
        float controlLeft(float x, float w) {
            return ax(x, w) - 6;
        }

        @Override
        void render(GuiGraphicsExtractor g, float x, float w, int mx, int my) {
            super.render(g, x, w, mx, my);
            boolean on = get.get();
            float ax = ax(x, w), ay = y + Grid.ROW / 2f;
            if (on && Draw.inRect(mx, my, ax - 6, y, 12, Grid.ROW)) Cursors.want(Cursors.Shape.HAND);
            int c = on ? Theme.GOLD : Theme.PANEL_LINE;
            if (openRow == this) Draw.triangle(g, ax - 3, ay - 1.5f, ax + 3, ay - 1.5f, ax, ay + 2f, c);
            else Draw.triangle(g, ax - 1.5f, ay - 3, ax - 1.5f, ay + 3, ax + 2f, ay, c);
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            if (Draw.inRect(mx, my, ax(x, w) - 6, y, 12, Grid.ROW)) {
                if (openRow == this) closeList();
                else if (get.get()) {
                    closeList();
                    openList = new ToggleList(langKey, spec.entries().get(), spec.off(), spec.onClosed());
                    openRow = this;
                    scroller.to(rowTop(this)); // the row as high as the list goes, so the entries get room
                }
                return true;
            }
            boolean took = super.click(mx, my, x, w);
            if (took && !get.get() && openRow == this) closeList(); // switched off: the list goes
            return took;
        }
    }

    /** Where row {@code row} starts in the list (scrolled 0). */
    private float rowTop(Row row) {
        float at = 0;
        for (Row r : rows) {
            if (r == row) return at;
            at += r.listHeight();
        }
        return 0;
    }

    private static class Toggle extends Row {
        final Supplier<Boolean> get;
        final Consumer<Boolean> set;

        Toggle(String key, Supplier<Boolean> get, Consumer<Boolean> set) {
            super(Component.translatable(key), Component.translatable(key + ".tooltip"));
            this.get = get;
            this.set = set;
        }

        private float tx(float x, float w) {
            return x + w - Grid.TOGGLE_W;
        }

        @Override
        float controlLeft(float x, float w) {
            return tx(x, w);
        }

        @Override
        String valueText() {
            return (get.get() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF).getString();
        }

        private float ty() {
            return y + (Grid.ROW - Grid.TOGGLE_H) / 2f;
        }

        @Override
        void render(GuiGraphicsExtractor g, float x, float w, int mx, int my) {
            renderLabel(g, x, w);
            Ui.toggle(g, tx(x, w), ty(), get.get(), mx, my);
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            if (!Ui.toggleHit(tx(x, w), ty(), mx, my)) return false;
            set.accept(!get.get());
            changed();
            return true;
        }
    }

    /**
     * An action ({@link AacsScreen.Tab#action}): the button's text in the reset icon's colours (muted, gold under the pointer) at the right end of an ordinary row, a small
     * arrow after it, underlined under the pointer (user choices 04.11.2026: a full button did not fit among the
     * settings); a click hands this screen to the mod.
     */
    private final class ActionRow extends Row {
        /** Room of the arrow after the text. */
        private static final int ARROW = 6;
        final Component button;
        final Consumer<Screen> onClick;

        ActionRow(String key, String buttonKey, Consumer<Screen> onClick) {
            super(Component.translatable(key), Component.translatable(key + ".tooltip"));
            this.button = Component.translatable(buttonKey);
            this.onClick = onClick;
        }

        @Override
        float controlLeft(float x, float w) {
            return x + w - ARROW - Grid.GAP - DrawText.width(button);
        }

        private boolean over(float x, float w, double mx, double my) {
            float left = controlLeft(x, w) - Grid.GAP;
            return Draw.inRect(mx, my, left, y, x + w - left, Grid.ROW);
        }

        @Override
        void renderLabel(GuiGraphicsExtractor g, float x, float w) {
            DrawText.text(g, DrawText.fit(label.getString(), (int) (controlLeft(x, w) - x - 2 * Grid.GAP)), x, DrawText.textY(y, Grid.ROW), Theme.WHITE, false);
        }

        @Override
        void render(GuiGraphicsExtractor g, float x, float w, int mx, int my) {
            renderLabel(g, x, w);
            boolean hover = over(x, w, mx, my);
            if (hover) Cursors.want(Cursors.Shape.HAND);
            int colour = hover ? Theme.GOLD : Theme.TEXT_MUTED;
            float tx = controlLeft(x, w), ty = DrawText.textY(y, Grid.ROW);
            DrawText.text(g, button, tx, ty, colour, false);
            if (hover) Draw.rect(g, tx, ty + 9, tx + DrawText.width(button), ty + 10, colour);
            float ax = x + w - 3, ay = y + Grid.ROW / 2f;
            Draw.triangle(g, ax - 1.5f, ay - 3, ax - 1.5f, ay + 3, ax + 2f, ay, colour);
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            if (!over(x, w, mx, my)) return false;
            onClick.accept(ConfigScreen.this);
            return true;
        }
    }

    /** Width of the value column right of a slider's track, at least. */
    private static final int VALUE_W = 32;

    /** A number or a choice in steps on a track, its value in words right of it (as Teyvat Cartographia's). */
    private static final class SliderRow extends Row {
        final AacsScreen.Slider s;
        final Supplier<Double> get;
        final Consumer<Double> set;
        float valueW = VALUE_W;
        /** The step names of every step row of the tab, this one's too. */
        List<String[]> stepGroup = List.of();

        SliderRow(String key, AacsScreen.Slider s, Supplier<Double> get, Consumer<Double> set) {
            super(Component.translatable(key), Component.translatable(key + ".tooltip"));
            this.s = s;
            this.get = get;
            this.set = set;
        }

        private int nearestStep(double v) {
            int best = 0;
            for (int i = 1; i < s.steps.length; i++) if (Math.abs(s.steps[i] - v) < Math.abs(s.steps[best] - v)) best = i;
            return best;
        }

        double toT(double v) {
            if (s.steps != null) return nearestStep(v) / (double) (s.steps.length - 1);
            return s.log ? (Math.log(v) - Math.log(s.min)) / (Math.log(s.maxNow()) - Math.log(s.min)) : (v - s.min) / (s.maxNow() - s.min);
        }

        double fromT(double t) {
            t = Math.max(0, Math.min(1, t));
            if (s.steps != null) return s.steps[(int) Math.round(t * (s.steps.length - 1))];
            double v = s.log ? Math.exp(Math.log(s.min) + t * (Math.log(s.maxNow()) - Math.log(s.min))) : s.min + t * (s.maxNow() - s.min);
            return s.integer ? Math.round(v) : Math.round(v * 100) / 100.0;
        }

        float trackX(float x, float w) {
            return x + w / 2;
        }

        float trackW(float w) {
            return w / 2 - valueW - Grid.PAD;
        }

        static float knobHalf() {
            return Theme.Style.squareUi ? 4 : 7;
        }

        @Override
        void render(GuiGraphicsExtractor g, float x, float w, int mx, int my) {
            renderLabel(g, x, w);
            if (s.stepKeys != null) {
                // The value column fits the longest step name of every step row of the tab (in the current language),
                // so their tracks end at the same place.
                int widest = 0;
                for (String[] keys : stepGroup) for (String k : keys) widest = Math.max(widest, DrawText.width(Component.translatable(k)));
                valueW = Math.max(VALUE_W, widest + Grid.GAP); // a gap between the knob at the end and the word
            }
            float tx0 = trackX(x, w), tw0 = trackW(w), cy = y + Grid.ROW / 2f;
            float half = knobHalf(), tx = tx0 + half, tw = tw0 - 2 * half;
            // A small mark at every step: choices in steps, a few fixed values, whole numbers with few values.
            double max = s.maxNow();
            int marks = s.stepKeys != null ? s.stepKeys.length : s.steps != null ? s.steps.length <= 9 ? s.steps.length : 0
                    : s.integer && max - s.min <= 8 ? (int) (max - s.min) + 1 : 0;
            if (marks > 1) {
                for (int i = 0; i < marks; i++) {
                    float sx = Math.round(tx + tw * i / (marks - 1f));
                    Draw.rect(g, sx - 0.5f, cy - 4, sx + 0.5f, cy + 4, Theme.SLIDER_TICK);
                }
            }
            double v = get.get();
            float t = (float) Math.max(0, Math.min(1, toT(v)));
            boolean hover = Draw.inRect(mx, my, tx0 - Grid.PAD, y, tw0 + 2 * Grid.PAD, Grid.ROW);
            if (hover) Cursors.want(Cursors.Shape.RESIZE_H);
            if (Theme.Style.squareUi) {
                Draw.rect(g, tx0, cy - 2, tx0 + tw0, cy + 2, Theme.TOGGLE_TRACK_OFF);
                Draw.rect(g, tx0, cy - 2, tx + tw * t, cy + 2, Theme.GOLD);
                float kx = Math.round(tx + tw * t), kh = hover ? 7 : 6;
                Draw.rect(g, kx - 4, cy - kh, kx + 4, cy + kh, Theme.TOGGLE_KNOB);
                Draw.frame(g, kx - 4, cy - kh, 8, 2 * kh, 1, Theme.SLIDER_TICK);
            } else {
                Draw.capsule(g, tx0, cy - 2, tw0, 4, Theme.TOGGLE_TRACK_OFF);
                Draw.capsule(g, tx0, cy - 2, Math.max(4, half + tw * t), 4, Theme.GOLD);
                float k = hover ? 7 : 6;
                Draw.diamond(g, tx + tw * t, cy, k, Theme.TOGGLE_KNOB);
                Draw.diamondOutline(g, tx + tw * t, cy, k, 1, Theme.SLIDER_TICK);
            }
            DrawText.textRight(g, Component.literal(DrawText.fit(text(v), (int) (valueW + Grid.GAP))), x + w, DrawText.textY(y, Grid.ROW), Theme.CREAM, false);
        }

        private String text(double v) {
            if (s.stepKeys != null) return Component.translatable(s.stepKeys[(int) Math.max(0, Math.min(s.stepKeys.length - 1, Math.round(v)))]).getString();
            if (s.labeler != null) return s.labeler.apply(v);
            String n = s.integer ? Long.toString(Math.round(v)) : String.format(java.util.Locale.ROOT, "%.2f", v);
            return n + s.unit;
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            if (!Draw.inRect(mx, my, trackX(x, w) - Grid.PAD, y, trackW(w) + 2 * Grid.PAD, Grid.ROW)) return false;
            drag(mx, x, w);
            return true;
        }

        @Override
        void drag(double mx, float x, float w) {
            float half = knobHalf();
            set.accept(fromT((mx - trackX(x, w) - half) / (trackW(w) - 2 * half)));
        }

        @Override
        void release() {
            if (s.onRelease != null) s.onRelease.run();
            changed();
        }

        @Override
        String valueText() {
            return text(get.get());
        }
    }

    /** A choice button with arrows at both ends: a click on its left or right half steps back or forward. */
    private static final class ChoiceRow<T> extends Row {
        static final int H = 16;
        final AacsScreen.Choice<T> c;
        final Supplier<T> get;
        final Consumer<T> set;

        ChoiceRow(String key, AacsScreen.Choice<T> c, Supplier<T> get, Consumer<T> set) {
            super(Component.translatable(key), Component.translatable(key + ".tooltip"));
            this.c = c;
            this.get = get;
            this.set = set;
        }

        float by() {
            return y + (Grid.ROW - H) / 2f;
        }

        @Override
        String valueText() {
            return c.names.apply(get.get()).getString();
        }

        /** It does something: active now, and more than one option to step through. */
        boolean active() {
            return c.activeWhen.getAsBoolean() && c.options.get().size() > 1;
        }

        @Override
        void render(GuiGraphicsExtractor g, float x, float w, int mx, int my) {
            renderLabel(g, x, w);
            float bx = controlLeft(x, w), bw = w / 2, by = by();
            boolean active = active(), hover = Draw.inRect(mx, my, bx, by, bw, H);
            if (hover) Cursors.want(active ? Cursors.Shape.HAND : Cursors.Shape.UNAVAILABLE);
            hover &= active;
            int fill = active ? hover ? Theme.BUTTON_HOVER : Theme.BUTTON : Ui.inactive(Theme.BUTTON);
            if (Theme.Style.squareUi) Draw.rect(g, bx, by, bx + bw, by + H, fill);
            else Draw.capsule(g, bx, by, bw, H, fill);
            Component name = Component.literal(DrawText.fit(c.names.apply(get.get()).getString(), (int) (bw - 2 * H)));
            DrawText.textCentered(g, name, bx + bw / 2, DrawText.textY(by, H), active ? Theme.CREAM : Ui.inactive(Theme.CREAM), false);
            float cy = by + H / 2f, a = H / 2f;
            int arrow = active ? Theme.GOLD : Ui.inactive(Theme.GOLD);
            Draw.triangle(g, bx + a - 2, cy, bx + a + 2, cy - 4, bx + a + 2, cy + 4, arrow);
            Draw.triangle(g, bx + bw - a + 2, cy, bx + bw - a - 2, cy - 4, bx + bw - a - 2, cy + 4, arrow);
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            float bx = controlLeft(x, w), bw = w / 2;
            if (!Draw.inRect(mx, my, bx, by(), bw, H)) return false;
            if (!active()) return true; // the click is taken, the value stays
            List<T> options = c.options.get();
            int cur = Math.max(0, options.indexOf(get.get()));
            int i = (cur + (mx < bx + bw / 2 ? -1 : 1) + options.size()) % options.size();
            set.accept(options.get(i));
            changed();
            return true;
        }
    }

    // ------------------------------------------------------------------ layout

    /** The card beside the panel: shown with at least this much room, at most this wide (GUI units). */
    private static final int CARD_MIN = 200, CARD_MAX = 300;
    private boolean card;
    private float cardX, cardW;
    /** The row the card (or the tooltip) tells of: the one under the pointer, kept while the pointer goes right. */
    private Row pointed;

    private void layout() {
        panelW = Math.min(PANEL_MAX, (width - 2 * 2 * Grid.MARGIN) / Grid.STEP * Grid.STEP);
        float rest = width - 2 * Grid.MARGIN - panelW - Grid.MARGIN;
        card = rest >= CARD_MIN;
        cardW = card ? Math.min(CARD_MAX, rest) : 0;
        panelX = Math.round((width - panelW - (card ? Grid.MARGIN + cardW : 0)) / 2f);
        cardX = panelX + panelW + Grid.MARGIN;
        panelY = Grid.MARGIN;
        panelH = height - 2 * Grid.MARGIN;
        listTop = tabs.size() > 1 ? tabY() + TAB_H + Grid.GAP : panelY + Grid.HEADER_H + Grid.GAP;
        listBottom = footerY() - Grid.PAD;
    }

    private float contentX() {
        return panelX + 2 * Grid.PAD;
    }

    private float contentW() {
        return panelW - 4 * Grid.PAD;
    }

    private float total() {
        float total = 0;
        for (Row r : rows) total += r.listHeight();
        return total;
    }

    private float maxScroll() {
        return Math.max(0, total() - (listBottom - listTop));
    }

    private boolean inView(Row r) {
        return r.shown() && r.y + r.height() > listTop && r.y < listBottom;
    }

    // ------------------------------------------------------------------ footer: the mod's buttons, Reset, Done

    private int buttonCount() {
        return spec.buttons.size() + 2;
    }

    private float footerY() {
        return panelY + panelH - Grid.PAD - Grid.ACTION_H;
    }

    private float footerButtonW() {
        int n = buttonCount();
        return (contentW() - (n - 1) * Grid.GAP) / n;
    }

    private float footerButtonX(int i) {
        return contentX() + i * (footerButtonW() + Grid.GAP);
    }

    // ------------------------------------------------------------------ scrollbar: in the right margin beside the list

    private float barX() {
        return contentX() + contentW() + Grid.PAD - BAR_W / 2f;
    }

    private float thumbH(float total) {
        float view = listBottom - listTop;
        return Math.max(Grid.ROW, view * view / total);
    }

    private float thumbY(float total) {
        float view = listBottom - listTop, th = thumbH(total);
        return listTop + (view - th) * Math.min(1, scroll / Math.max(1, maxScroll()));
    }

    private boolean barHit(double mx, double my) {
        return total() > listBottom - listTop && my >= listTop && my < listBottom && mx >= barX() - Grid.GAP && mx < barX() + BAR_W + Grid.GAP;
    }

    private void dragBar(double my) {
        float total = total(), view = listBottom - listTop, th = thumbH(total);
        scroller.toFraction((float) ((my - barGrab - listTop) / (view - th)));
    }

    private void renderBar(GuiGraphicsExtractor g, float total, int mx, int my) {
        float view = listBottom - listTop;
        if (total <= view) return;
        boolean hover = barHit(mx, my);
        if (hover) Cursors.want(Cursors.Shape.RESIZE_V);
        float bx = barX(), th = thumbH(total), ty = thumbY(total);
        int thumb = hover || draggingBar ? Theme.GOLD : Theme.SCROLL_THUMB;
        if (Theme.Style.squareUi) {
            Draw.rect(g, bx, listTop, bx + BAR_W, listBottom, Theme.SCROLL_TRACK);
            Draw.rect(g, bx, ty, bx + BAR_W, ty + th, thumb);
        } else {
            Draw.roundRect(g, bx, listTop, BAR_W, view, BAR_W / 2f, Theme.SCROLL_TRACK);
            Draw.roundRect(g, bx, ty, BAR_W, th, BAR_W / 2f, thumb);
        }
    }

    // ------------------------------------------------------------------ tabs: widths follow their titles

    private float tabY() {
        return panelY + Grid.HEADER_H + Grid.GAP;
    }

    private float tabNeed(int i) {
        return DrawText.width(Component.translatable(spec.tabs.get(i).key)) + 4 * Grid.GAP;
    }

    private float tabW(int i) {
        float gaps = (tabs.size() - 1) * Grid.GAP, need = 0;
        for (int k = 0; k < tabs.size(); k++) need += tabNeed(k);
        float room = contentW() - gaps;
        return room >= need ? tabNeed(i) + (room - need) / tabs.size() : tabNeed(i) * room / need;
    }

    private float tabX(int i) {
        float x = contentX();
        for (int k = 0; k < i; k++) x += tabW(k) + Grid.GAP;
        return x;
    }

    private void renderTab(GuiGraphicsExtractor g, int i, int mx, int my) {
        float x = tabX(i), y = tabY(), w = tabW(i);
        boolean shown = i == tab, hover = !shown && Draw.inRect(mx, my, x, y, w, TAB_H);
        if (hover) Cursors.want(Cursors.Shape.HAND);
        int fill = shown || hover ? Theme.BUTTON : Ui.inactive(Theme.BUTTON);
        if (Theme.Style.squareUi) Draw.rect(g, x, y, x + w, y + TAB_H, fill);
        else Draw.capsule(g, x, y, w, TAB_H, fill);
        Component t = Component.literal(DrawText.fit(Component.translatable(spec.tabs.get(i).key).getString(), (int) (w - 2 * Grid.GAP)));
        DrawText.textCentered(g, t, x + w / 2f, DrawText.textY(y, TAB_H), shown ? Theme.GOLD : Theme.CREAM, false);
    }

    // ------------------------------------------------------------------ the card beside the list

    /** The row under the pointer; kept while the pointer is right of the list (its bar, the gap, the card). */
    private void followPointer(int mx, int my) {
        if (draggingRow != null) {
            pointed = draggingRow;
            return;
        }
        if (mx >= contentX() + contentW()) return; // right of the list (to the card): the row stays
        // Over the open list: the row it opened under (not the rows hidden under the list).
        if (openList != null && openList.over(mx, my)) {
            pointed = openRow;
            return;
        }
        Row at = null;
        if (my >= listTop && my < listBottom && mx >= contentX()) {
            for (Row r : rows) if (r.tooltip != null && inView(r) && my >= r.y && my < r.y + r.height()) at = r;
        }
        pointed = at;
    }

    /**
     * Without room for the card: the description of the row under the pointer as a tooltip under or over the row, none
     * while a slider or the tooltip's own bar is dragged or over the open list; one that scrolls stays while the
     * pointer is on it.
     */
    private void followTooltip(int mx, int my) {
        if (tip.dragging() || tip.scrolls() && tip.contains(mx, my)) return;
        if (scroll != scroller.target()) {
            tip.hide(); // the rows still move: placed now, it would stay behind them
            return;
        }
        Row at = null;
        if (draggingRow == null && (openList == null || !openList.over(mx, my)) && my >= listTop && my < listBottom
                && mx >= contentX() && mx < contentX() + contentW()) {
            for (Row r : rows) if (r.tooltip != null && inView(r) && my >= r.y && my < r.y + r.height()) at = r;
        }
        if (at == null) tip.hide();
        else tip.show(at, at.label, at.tooltip, mx, at.y, at.y + at.height(), width, Grid.MARGIN, footerY() - Grid.GAP);
    }

    private float roomW, roomH;
    private String roomLanguage;

    /**
     * Height of the longest description (name, line and text) among the rows with pictures of every tab, {@code w}
     * wide: the pictures start at the same place for every setting, whatever the length of its own text.
     */
    private float descriptionRoom(float w) {
        String language = Minecraft.getInstance().options.languageCode;
        if (language.equals(roomLanguage) && w == roomW) return roomH;
        int most = 0;
        for (List<Row> tab : tabs) {
            for (Row r : tab) if (r.tooltip != null && r.preview != null) most = Math.max(most, font.split(r.tooltip, (int) w).size());
        }
        roomLanguage = language;
        roomW = w;
        roomH = DrawText.LINE + 2 * Grid.GAP + 1 + most * (DrawText.LINE + 2) - 2;
        return roomH;
    }

    /** The area of the card's pictures: x, y, w, h (the whole room under the description, without the caption). */
    private float cardInnerX() {
        return cardX + Grid.PAD;
    }

    private float cardInnerY() {
        return panelY + Grid.HEADER_H + Grid.PAD;
    }

    private float cardInnerW() {
        return cardW - 2 * Grid.PAD;
    }

    private float cardInnerH() {
        return panelH - Grid.HEADER_H - 2 * Grid.PAD;
    }

    /**
     * The card: a dark panel with its title, then for the setting pointed at its name and description, its pictures
     * under them (a caption with its value under a picture that has none of its own); with none pointed at, a line
     * asking for one.
     */
    private void renderCard(GuiGraphicsExtractor g, int mx, int my, float pt) {
        float x = cardX, y = panelY, w = cardW, h = panelH;
        Draw.rect(g, x, y, x + w, y + h, Theme.PANEL_DARK);
        Draw.rect(g, x, y, x + w, y + Grid.HEADER_H, Theme.PANEL_DARK_HEADER);
        Draw.rect(g, x, y + Grid.HEADER_H - 1, x + w, y + Grid.HEADER_H, Theme.PANEL_HEADER_LINE);
        DrawText.room(spec.cardTitle, w - 2 * Grid.PAD);
        DrawText.text(g, spec.cardTitle, x + Grid.PAD, DrawText.textY(y, Grid.HEADER_H), Theme.GOLD, false);
        float cx = cardInnerX(), cy = cardInnerY(), cw = cardInnerW(), ch = cardInnerH();
        Row row = pointed;
        if (row == null || row.tooltip == null) {
            cardLines(g, cx, cy, cw, ch, spec.cardPrompt);
            return;
        }
        // The description at the top: the name in gold over a line, the text wrapped under it.
        DrawText.text(g, DrawText.fit(row.label.getString(), (int) cw), cx, cy, Theme.GOLD, false);
        Draw.rect(g, cx, cy + DrawText.LINE + Grid.GAP, cx + cw, cy + DrawText.LINE + Grid.GAP + 1, Theme.PANEL_LINE);
        float ty = cy + DrawText.LINE + 2 * Grid.GAP + 1;
        for (FormattedCharSequence l : font.split(row.tooltip, (int) cw)) {
            g.pose().pushMatrix();
            g.pose().translate(Draw.snap(cx), Draw.snap(ty));
            g.text(font, l, 0, 0, Theme.CREAM, false);
            g.pose().popMatrix();
            ty += DrawText.LINE + 2;
        }
        if (row.preview == null) return;
        // The pictures under it, from where the longest description of all ends: the same place and size for every
        // setting, whatever the length of its own text.
        float top = Draw.snap(cy + descriptionRoom(cw) + Grid.PAD);
        boolean caption = row.preview.caption(row.langKey);
        float captionH = caption ? DrawText.LINE + Grid.GAP : 0;
        float vh = Draw.snap(cy + ch - top - captionH);
        g.enableScissor((int) cx, (int) top, (int) (cx + cw), (int) (top + vh)); // nothing spills out of the area
        row.preview.draw(g, row.langKey, cx, top, cw, vh, pt);
        g.disableScissor();
        if (caption) {
            String text = row.label.getString() + (row.valueText() == null ? "" : ": " + row.valueText());
            DrawText.room(text, cw);
            DrawText.textCentered(g, Component.literal(DrawText.fit(text, (int) cw)), Draw.snap(cx + cw / 2f), top + vh + Grid.GAP, Theme.CREAM, false);
        }
    }

    /** Lines of muted text in the middle of the area, each shortened to fit. */
    private static void cardLines(GuiGraphicsExtractor g, float x, float y, float w, float h, List<Component> lines) {
        float top = y + (h - lines.size() * (DrawText.LINE + 2) + 2) / 2f;
        for (int i = 0; i < lines.size(); i++) {
            Component t = Component.literal(DrawText.fit(lines.get(i).getString(), (int) w));
            DrawText.textCentered(g, t, Draw.snap(x + w / 2f), Draw.snap(top + i * (DrawText.LINE + 2)), Theme.TEXT_MUTED, false);
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        if (spec.background != null) spec.background.draw(g, width, height, pt);
        else super.extractBackground(g, mx, my, pt);
    }

    /** Development checks only: where the pointer is taken to be (GUI units) instead of the mouse; null in play. */
    public static int[] testPointer;

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        if (testPointer != null) {
            mx = testPointer[0];
            my = testPointer[1];
        }
        Theme.use(spec.palette.get(), !spec.rounded.getAsBoolean());
        Cursors.begin();
        // The background is drawn by the game before this (in a stratum of its own, Screen.extractRenderStateWithTooltipAndSubtitles).
        layout();
        if (spec.background != null) {
            // The mod's own background blurred under the panel, as the game blurs the world (the copy is reused while
            // nothing changes: refreshed for the first frames, after a resize and after a change of colours).
            boolean refresh = blurFrames < 3 || width != blurW || height != blurH || Theme.generation() != blurTheme;
            int bx = mx, by = my;
            Backdrop.capture(g, pt, refresh, this, () -> extractBackground(g, bx, by, pt));
            blurFrames++;
            blurW = width;
            blurH = height;
            blurTheme = Theme.generation();
        }
        Draw.rect(g, 0, 0, width, height, Theme.SETTINGS_DIM);
        // The card's picture is prepared before anything is clipped (it may draw into a layer of its own).
        if (card && pointed != null && pointed.preview != null) pointed.preview.prepare(g, pointed.langKey, cardInnerW(), cardInnerH(), pt);
        Ui.panel(g, panelX, panelY, panelW, panelH, title, titleIcon, mx, my);
        if (card) Backdrop.draw(g, cardX, panelY, cardX + cardW, panelY + panelH);
        Backdrop.release();
        if (tabs.size() > 1) for (int i = 0; i < tabs.size(); i++) renderTab(g, i, mx, my);
        float total = total();
        scroll = scroller.update(maxScroll());

        EdgeFade.capture(g);
        g.enableScissor((int) panelX, (int) listTop, (int) (panelX + panelW), (int) listBottom);
        float yy = listTop - scroll;
        // A scrolling tooltip holds the pointer: the rows under it are not pointed at.
        boolean onTip = !card && tip.scrolls() && tip.contains(mx, my);
        int rowMy = my >= listTop && my < listBottom && !onTip && (openList == null || !openList.over(mx, my)) ? my : -10_000;
        for (Row r : rows) {
            r.y = yy;
            if (r.shown() && yy + r.height() > listTop && yy < listBottom) {
                r.render(g, contentX(), contentW(), mx, rowMy);
                r.renderReset(g, contentX(), contentW(), mx, rowMy);
            }
            yy += r.listHeight();
        }
        g.disableScissor();
        float view = listBottom - listTop;
        EdgeFade.draw(g, panelX, barX() - Grid.GAP, listTop, listBottom, FADE,
                Math.min(1, scroll / FADE), Math.min(1, Math.max(0, total - scroll - view) / FADE));
        EdgeFade.release();
        // The open list, under its row and over the rows below it; it closes when its row leaves the view (where the
        // list is going: it may slide in while the list opens).
        if (openList != null) {
            float target = listTop + rowTop(openRow) - scroller.target();
            if (!rows.contains(openRow) || !openRow.shown() || target < listTop - 0.5f || target + Grid.ROW > listBottom + 0.5f) closeList();
            else {
                float top = openRow.y + Grid.ROW;
                g.enableScissor((int) panelX, (int) listTop, (int) (panelX + panelW), (int) listBottom);
                openList.render(g, contentX(), top, contentW(), openList.height(listBottom - top), mx, my);
                g.disableScissor();
            }
        }
        renderBar(g, total, mx, my);
        followPointer(mx, my);
        if (card) renderCard(g, mx, my, pt);

        float bw = footerButtonW(), by = footerY();
        for (int i = 0; i < spec.buttons.size(); i++) {
            Ui.actionButton(g, footerButtonX(i), by, bw, Component.translatable(spec.buttons.get(i).key()), buttonIcons.get(i), true, mx, my);
        }
        int n = spec.buttons.size();
        Ui.actionButton(g, footerButtonX(n), by, bw, Component.translatable("aacs.reset"), Icon.RESET, true, mx, my);
        Ui.actionButton(g, footerButtonX(n + 1), by, bw, CommonComponents.GUI_DONE, Icon.CHECK, true, mx, my);

        // Without room for the card, the description of the row under the pointer as a tooltip, over everything.
        if (card) tip.hide();
        else {
            followTooltip(mx, my);
            tip.render(g, mx, my);
        }
        if (draggingRow != null) Cursors.force(Cursors.Shape.RESIZE_H);
        if (draggingBar) Cursors.force(Cursors.Shape.RESIZE_V);
        Cursors.apply();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        layout();
        if (openList != null && openList.click(mx, my, button)) return true;
        if (Ui.closeHit(panelX, panelY, panelW, mx, my)) {
            onClose();
            return true;
        }
        // A scrolling tooltip takes the clicks on it (its bar), none reach the rows under it.
        if (!card && tip.click(mx, my)) return true;
        for (int i = 0; tabs.size() > 1 && i < tabs.size(); i++) {
            if (Draw.inRect(mx, my, tabX(i), tabY(), tabW(i), TAB_H)) {
                closeList();
                tip.hide();
                tab = i;
                SHOWN_TAB.put(spec.modId, i);
                rows = tabs.get(i);
                scroller.jump(0);
                scroll = 0f;
                return true;
            }
        }
        float bw = footerButtonW(), by = footerY();
        for (int i = 0; i < spec.buttons.size(); i++) {
            if (Ui.actionHit(footerButtonX(i), by, bw, mx, my)) {
                spec.buttons.get(i).onClick().accept(this);
                return true;
            }
        }
        int n = spec.buttons.size();
        if (Ui.actionHit(footerButtonX(n), by, bw, mx, my)) {
            for (Value<?> v : spec.file.values()) v.reset();
            for (Value<?> v : extraValues) v.reset();
            spec.onReset.run();
            return true;
        }
        if (Ui.actionHit(footerButtonX(n + 1), by, bw, mx, my)) {
            onClose();
            return true;
        }
        if (barHit(mx, my)) {
            float th = thumbH(total()), ty = thumbY(total());
            barGrab = my >= ty && my < ty + th ? (float) my - ty : th / 2f; // on the track: the thumb jumps under the pointer
            draggingBar = true;
            dragBar(my);
            return true;
        }
        if (my >= listTop && my < listBottom) {
            for (Row r : rows) {
                if (!inView(r)) continue;
                if (r.resetClick(mx, my, contentX(), contentW())) return true;
                if (r.click(mx, my, contentX(), contentW())) {
                    if (r instanceof SliderRow) draggingRow = r;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (tip.drag(my)) return true;
        if (draggingBar) {
            dragBar(my);
            return true;
        }
        if (draggingRow != null) {
            draggingRow.drag(mx, contentX(), contentW());
            return true;
        }
        if (openList != null && openList.drag(my)) return true;
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingRow != null) draggingRow.release();
        draggingRow = null;
        draggingBar = false;
        tip.release();
        if (openList != null) openList.release();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (openList != null && openList.over(mx, my)) {
            openList.scrollBy(sy);
            return true;
        }
        if (!card && tip.scroll(mx, my, sy)) return true;
        tip.hide(); // the rows move under it
        scroller.wheel(sy, WHEEL);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return openList != null && openList.charTyped((char) event.codepoint(), 0) || super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key(), scan = Game.scancode(event), mods = event.modifiers();
        if (openList != null && openList.keyPressed(key, scan, mods)) return true;
        if (key == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    // ------------------------------------------------------------------ the GUI scale cap, closing

    private boolean scaleCapped;

    /**
     * The settings are never larger than GUI 3 at 1080p (Teyvat Cartographia, user decision 31.10.2026): the scale is
     * at most the window's height over 360, whole. Also called again on every resize of the window.
     */
    @Override
    protected void init() {
        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        int cap = scaleCap(window.getHeight());
        if (window.getGuiScale() > cap) {
            window.setGuiScale(cap);
            width = window.getGuiScaledWidth();
            height = window.getGuiScaledHeight();
            scaleCapped = true;
        }
    }

    /** The largest GUI scale of the settings for a window {@code h} pixels high: 3 at 1080p, 4 at 1440p, 6 at 2160p. */
    public static int scaleCap(int h) {
        return Math.max(1, h / 360);
    }

    @Override
    public void removed() {
        closeList();
        Cursors.reset();
        Backdrop.dispose();
        EdgeFade.dispose();
        if (scaleCapped) {
            Minecraft mc = Minecraft.getInstance();
            mc.getWindow().setGuiScale(mc.getWindow().calculateScale(mc.options.guiScale().get(), mc.options.forceUnicodeFont().get()));
            scaleCapped = false;
        }
    }

    @Override
    public void onClose() {
        spec.file.save();
        for (net.tapacywka.aacs.api.ConfigFile f : extraFiles) if (f != spec.file) f.save();
        Game.setScreen(parent);
    }
}
