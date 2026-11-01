package net.tapacywka.aacs.impl.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.tapacywka.aacs.api.AacsScreen;
import net.tapacywka.aacs.api.Value;
import net.tapacywka.aacs.impl.gui.Ui.Grid;
import net.tapacywka.aacs.impl.render.Backdrop;
import net.tapacywka.aacs.impl.render.Draw;
import net.tapacywka.aacs.impl.render.DrawText;
import net.tapacywka.aacs.impl.render.EdgeFade;
import net.tapacywka.aacs.impl.render.Icon;
import net.tapacywka.aacs.impl.render.Theme;
import org.lwjgl.glfw.GLFW;

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
        for (AacsScreen.Tab t : spec.tabs) {
            List<Row> list = new ArrayList<>();
            SubSection owner = null;
            for (AacsScreen.Row r : t.rows) {
                Row row = switch (r.kind()) {
                    case SECTION -> new Section(r.key());
                    case SUB_SECTION -> new SubSection(spec.modId + "/" + r.key(), r.label() != null ? r.label() : Component.translatable(r.key()));
                    case TOGGLE -> new Toggle(r.key(), cast(r.get()), cast(r.set()));
                    case TOGGLE_LIST -> new ListRow(r.key(), cast(r.get()), cast(r.set()), cast(r.options()));
                    case SLIDER -> new SliderRow(r.key(), cast(r.options()), cast(r.get()), cast(r.set()));
                    case CHOICE -> new ChoiceRow<>(r.key(), cast(r.options()), cast(r.get()), cast(r.set()));
                };
                row.value = r.value();
                row.preview = r.preview();
                if (row instanceof Section) owner = null;
                else if (row instanceof SubSection s) owner = s;
                else row.owner = owner;
                list.add(row);
            }
            tabs.add(list);
        }
        tab = Math.min(SHOWN_TAB.getOrDefault(spec.modId, 0), tabs.size() - 1);
        rows = tabs.get(tab);
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

        /** The value as the row shows it ("On", "40%", "Top left"), for the card's caption; null for headings. */
        String valueText() {
            return null;
        }

        void renderLabel(GuiGraphics g, float x, float w) {
            // Short of the middle by the reset icon too, so the label never jumps when the icon shows.
            DrawText.text(g, DrawText.fit(label.getString(), (int) (w / 2 - 2 * Grid.GAP - RESET)), x, DrawText.textY(y, Grid.ROW), Theme.WHITE, false);
        }

        abstract void render(GuiGraphics g, float x, float w, int mx, int my);

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

        void renderReset(GuiGraphics g, float x, float w, int mx, int my) {
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
        void render(GuiGraphics g, float x, float w, int mx, int my) {
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
        void render(GuiGraphics g, float x, float w, int mx, int my) {
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
        void render(GuiGraphics g, float x, float w, int mx, int my) {
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
                    openList = new ToggleList(spec.entries().get(), spec.off(), spec.onClosed());
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
        void render(GuiGraphics g, float x, float w, int mx, int my) {
            renderLabel(g, x, w);
            Ui.toggle(g, tx(x, w), ty(), get.get(), mx, my);
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            if (!Ui.toggleHit(tx(x, w), ty(), mx, my)) return false;
            set.accept(!get.get());
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
            return s.log ? (Math.log(v) - Math.log(s.min)) / (Math.log(s.max) - Math.log(s.min)) : (v - s.min) / (s.max - s.min);
        }

        double fromT(double t) {
            t = Math.max(0, Math.min(1, t));
            if (s.steps != null) return s.steps[(int) Math.round(t * (s.steps.length - 1))];
            double v = s.log ? Math.exp(Math.log(s.min) + t * (Math.log(s.max) - Math.log(s.min))) : s.min + t * (s.max - s.min);
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
        void render(GuiGraphics g, float x, float w, int mx, int my) {
            renderLabel(g, x, w);
            if (s.stepKeys != null) {
                int widest = 0;
                for (String k : s.stepKeys) widest = Math.max(widest, DrawText.width(Component.translatable(k)));
                valueW = Math.max(VALUE_W, widest + Grid.GAP);
            }
            float tx0 = trackX(x, w), tw0 = trackW(w), cy = y + Grid.ROW / 2f;
            float half = knobHalf(), tx = tx0 + half, tw = tw0 - 2 * half;
            int marks = s.stepKeys != null ? s.stepKeys.length : s.steps != null ? s.steps.length
                    : s.integer && s.max - s.min <= 8 ? (int) (s.max - s.min) + 1 : 0;
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
        void render(GuiGraphics g, float x, float w, int mx, int my) {
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

    private void renderBar(GuiGraphics g, float total, int mx, int my) {
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

    private void renderTab(GuiGraphics g, int i, int mx, int my) {
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
        boolean overList = my >= listTop && my < listBottom && mx >= contentX() && mx < contentX() + contentW()
                && (openList == null || !openList.over(mx, my));
        if (overList) {
            pointed = null;
            for (Row r : rows) {
                if (r.tooltip != null && inView(r) && my >= r.y && my < r.y + r.height()) pointed = r;
            }
        } else if (!(mx >= contentX() + contentW() && my >= panelY && my < panelY + panelH)) {
            pointed = null;
        }
    }

    /** Where the pictures start under the name and description: below the longest description of the screen. */
    private float descriptionRoom;
    private String roomKey;

    private float descriptionRoom(float textW) {
        String key = Minecraft.getInstance().getLanguageManager().getSelected() + "/" + (int) textW;
        if (!key.equals(roomKey)) {
            roomKey = key;
            int most = 0;
            for (List<Row> tab : tabs) {
                for (Row r : tab) if (r.tooltip != null) most = Math.max(most, font.split(r.tooltip, (int) textW).size());
            }
            descriptionRoom = Grid.ROW + most * DrawText.LINE + Grid.PAD;
        }
        return descriptionRoom;
    }

    private void renderCard(GuiGraphics g, int mx, int my, float pt) {
        float x = cardX, y = panelY, w = cardW, h = panelH;
        Draw.rect(g, x, y, x + w, y + h, Theme.PANEL_DARK);
        float tx = x + 2 * Grid.PAD, tw = w - 4 * Grid.PAD, ty = y + 2 * Grid.PAD;
        if (pointed == null) {
            Component none = Component.translatable("aacs.preview.none");
            DrawText.textCentered(g, Component.literal(DrawText.fit(none.getString(), (int) tw)), x + w / 2, y + h / 2 - 4, Theme.TEXT_MUTED, false);
            return;
        }
        DrawText.text(g, DrawText.fit(pointed.label.getString(), (int) tw), tx, ty, Theme.GOLD, false);
        float ly = ty + Grid.ROW;
        for (var line : font.split(pointed.tooltip, (int) tw)) {
            g.drawString(font, line, Math.round(tx), Math.round(ly), Theme.CREAM, false);
            ly += DrawText.LINE;
        }
        String value = pointed.valueText();
        float picTop = ty + descriptionRoom(tw), captionH = value != null ? Grid.ROW : 0;
        float picH = y + h - 2 * Grid.PAD - captionH - picTop;
        if (pointed.preview != null && picH > Grid.ROW) {
            g.enableScissor((int) tx, (int) picTop, (int) (tx + tw), (int) (picTop + picH));
            pointed.preview.draw(g, tx, picTop, tw, picH, pt);
            g.disableScissor();
        }
        if (value != null) {
            Component caption = Component.literal(DrawText.fit(pointed.label.getString() + ": " + value, (int) tw));
            DrawText.textCentered(g, caption, x + w / 2, DrawText.textY(y + h - 2 * Grid.PAD - Grid.ROW, Grid.ROW), Theme.TEXT_MUTED, false);
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        if (spec.background != null) spec.background.draw(g, width, height, pt);
        else super.renderBackground(g, mx, my, pt);
    }

    /** Development checks only: where the pointer is taken to be (GUI units) instead of the mouse; null in play. */
    public static int[] testPointer;

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (testPointer != null) {
            mx = testPointer[0];
            my = testPointer[1];
        }
        Theme.use(spec.palette, !spec.rounded.getAsBoolean());
        Cursors.begin();
        renderBackground(g, mx, my, pt);
        layout();
        if (spec.background != null) {
            // The mod's own background blurred under the panel, as the game blurs the world (the copy is reused while
            // nothing changes: refreshed for the first frames, after a resize and after a change of colours).
            boolean refresh = blurFrames < 3 || width != blurW || height != blurH || Theme.generation() != blurTheme;
            Backdrop.capture(g, pt, refresh, this);
            blurFrames++;
            blurW = width;
            blurH = height;
            blurTheme = Theme.generation();
        }
        Draw.rect(g, 0, 0, width, height, Theme.SETTINGS_DIM);
        // The card's picture is prepared before anything is clipped (it may draw into a layer of its own).
        if (card && pointed != null && pointed.preview != null) pointed.preview.prepare(cardW - 4 * Grid.PAD, panelH, pt);
        Ui.panel(g, panelX, panelY, panelW, panelH, title, titleIcon, mx, my);
        if (card) Backdrop.draw(g, cardX, panelY, cardX + cardW, panelY + panelH);
        Backdrop.release();
        if (tabs.size() > 1) for (int i = 0; i < tabs.size(); i++) renderTab(g, i, mx, my);
        float total = total();
        scroll = scroller.update(maxScroll());

        EdgeFade.capture(g);
        g.enableScissor((int) panelX, (int) listTop, (int) (panelX + panelW), (int) listBottom);
        float yy = listTop - scroll;
        int rowMy = my >= listTop && my < listBottom && (openList == null || !openList.over(mx, my)) ? my : -10_000;
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
        if (!card && pointed != null && draggingRow == null && my >= pointed.y && my < pointed.y + pointed.height()) {
            g.renderTooltip(font, font.split(pointed.tooltip, Math.max(120, (int) (panelW / 2))), mx, my);
        }
        if (draggingRow != null) Cursors.force(Cursors.Shape.RESIZE_H);
        if (draggingBar) Cursors.force(Cursors.Shape.RESIZE_V);
        Cursors.apply();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        layout();
        if (openList != null && openList.click(mx, my, button)) return true;
        if (Ui.closeHit(panelX, panelY, panelW, mx, my)) {
            onClose();
            return true;
        }
        for (int i = 0; tabs.size() > 1 && i < tabs.size(); i++) {
            if (Draw.inRect(mx, my, tabX(i), tabY(), tabW(i), TAB_H)) {
                closeList();
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
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingBar) {
            dragBar(my);
            return true;
        }
        if (draggingRow != null) {
            draggingRow.drag(mx, contentX(), contentW());
            return true;
        }
        if (openList != null && openList.drag(my)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingRow != null) draggingRow.release();
        draggingRow = null;
        draggingBar = false;
        if (openList != null) openList.release();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (openList != null && openList.over(mx, my)) {
            openList.scrollBy(sy);
            return true;
        }
        scroller.wheel(sy, WHEEL);
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        return openList != null && openList.charTyped(c, mods) || super.charTyped(c, mods);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (openList != null && openList.keyPressed(key, scan, mods)) return true;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
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
        Minecraft.getInstance().setScreen(parent);
    }
}
