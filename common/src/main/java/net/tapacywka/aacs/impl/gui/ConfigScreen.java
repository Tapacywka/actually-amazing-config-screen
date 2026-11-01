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
                };
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

        void renderLabel(GuiGraphics g, float x, float w) {
            DrawText.text(g, DrawText.fit(label.getString(), (int) (w / 2 - Grid.GAP)), x, DrawText.textY(y, Grid.ROW), Theme.WHITE, false);
        }

        abstract void render(GuiGraphics g, float x, float w, int mx, int my);

        boolean click(double mx, double my, float x, float w) {
            return false;
        }
    }

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

    private static final class Toggle extends Row {
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

    // ------------------------------------------------------------------ layout

    private void layout() {
        panelW = Math.min(PANEL_MAX, (width - 2 * 2 * Grid.MARGIN) / Grid.STEP * Grid.STEP);
        panelX = Math.round((width - panelW) / 2f);
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

    // ------------------------------------------------------------------ drawing

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        if (spec.background != null) spec.background.draw(g, width, height, pt);
        else super.renderBackground(g, mx, my, pt);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
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
        Ui.panel(g, panelX, panelY, panelW, panelH, title, titleIcon, mx, my);
        Backdrop.release();
        if (tabs.size() > 1) for (int i = 0; i < tabs.size(); i++) renderTab(g, i, mx, my);
        float total = total();
        scroll = scroller.update(maxScroll());

        EdgeFade.capture(g);
        g.enableScissor((int) panelX, (int) listTop, (int) (panelX + panelW), (int) listBottom);
        float yy = listTop - scroll;
        int rowMy = my >= listTop && my < listBottom ? my : -10_000;
        for (Row r : rows) {
            r.y = yy;
            if (r.shown() && yy + r.height() > listTop && yy < listBottom) r.render(g, contentX(), contentW(), mx, rowMy);
            yy += r.listHeight();
        }
        g.disableScissor();
        float view = listBottom - listTop;
        EdgeFade.draw(g, panelX, barX() - Grid.GAP, listTop, listBottom, FADE,
                Math.min(1, scroll / FADE), Math.min(1, Math.max(0, total - scroll - view) / FADE));
        EdgeFade.release();
        renderBar(g, total, mx, my);

        float bw = footerButtonW(), by = footerY();
        for (int i = 0; i < spec.buttons.size(); i++) {
            Ui.actionButton(g, footerButtonX(i), by, bw, Component.translatable(spec.buttons.get(i).key()), buttonIcons.get(i), true, mx, my);
        }
        int n = spec.buttons.size();
        Ui.actionButton(g, footerButtonX(n), by, bw, Component.translatable("aacs.reset"), Icon.RESET, true, mx, my);
        Ui.actionButton(g, footerButtonX(n + 1), by, bw, CommonComponents.GUI_DONE, Icon.CHECK, true, mx, my);

        if (draggingBar) Cursors.force(Cursors.Shape.RESIZE_V);
        Cursors.apply();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        layout();
        if (Ui.closeHit(panelX, panelY, panelW, mx, my)) {
            onClose();
            return true;
        }
        for (int i = 0; tabs.size() > 1 && i < tabs.size(); i++) {
            if (Draw.inRect(mx, my, tabX(i), tabY(), tabW(i), TAB_H)) {
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
                if (inView(r) && r.click(mx, my, contentX(), contentW())) return true;
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
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingBar = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroller.wheel(sy, WHEEL);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
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
