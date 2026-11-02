package net.tapacywka.aacs.impl.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.tapacywka.aacs.api.AacsScreen;
import net.tapacywka.aacs.api.ListValue;
import net.tapacywka.aacs.impl.gui.Ui.Grid;
import net.tapacywka.aacs.impl.render.Draw;
import net.tapacywka.aacs.impl.render.DrawText;
import net.tapacywka.aacs.impl.render.EdgeFade;
import net.tapacywka.aacs.impl.render.Icon;
import net.tapacywka.aacs.impl.render.Theme;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The list under a switch row opened by its arrow (Teyvat Cartographia's list of plants): a field at the top that
 * filters it by name as you type, with how many are on, then every entry with its colour, name and a switch. The
 * entries switched off are the ids in a {@link ListValue}. Drawn over the rows below its row; scrolled by the wheel.
 */
final class ToggleList {
    private static final int ITEM_H = 20, MAX_ROWS = 9;
    private static final float FADE = 8, BAR_W = 3;

    private record Entry(AacsScreen.ListEntry e, String lower) {}

    private final ListValue off;
    private final Runnable onClosed;
    private final ChipField field;
    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> shown = new ArrayList<>();
    private String filter = "";
    private final SmoothScroll scroller = new SmoothScroll();
    /** A switch changed since opening: {@link #onClosed} runs once the list closes. */
    private boolean changed;
    private boolean dragging;
    private float grab;
    // Where the list was drawn last (for the mouse).
    private float x, y, w, h;

    /** The lang key of the row it opens under: the mod's own texts ({@code <key>.find}, {@code .count}, {@code .none}) first. */
    private final String rowKey;

    ToggleList(String rowKey, List<AacsScreen.ListEntry> entries, ListValue off, Runnable onClosed) {
        this.rowKey = rowKey;
        this.off = off;
        this.onClosed = onClosed;
        Component hint = text("find");
        field = new ChipField(ChipField.Style.DARK, Icon.SEARCH, hint, hint, 32);
        field.setResponder(s -> {
            filter = s.toLowerCase(Locale.ROOT).trim();
            scroller.jump(0);
            refilter();
        });
        for (AacsScreen.ListEntry e : entries) all.add(new Entry(e, e.name().getString().toLowerCase(Locale.ROOT)));
        all.sort(Comparator.comparing(Entry::lower));
        refilter();
    }

    private void refilter() {
        shown.clear();
        for (Entry e : all) if (filter.isEmpty() || e.lower.contains(filter)) shown.add(e);
    }

    private boolean on(Entry e) {
        return !off.get().contains(e.e.id());
    }

    private void set(Entry e, boolean on) {
        List<String> now = new ArrayList<>(off.get());
        now.remove(e.e.id());
        if (!on) now.add(e.e.id());
        off.set(now);
        changed = true;
    }

    /** Height the list takes with its rows (at most {@code room}). */
    float height(float room) {
        return Math.min(room, 2 * Grid.GAP + Grid.CHIP_H + Grid.GAP + Math.min(MAX_ROWS, Math.max(1, shown.size())) * ITEM_H + Grid.GAP);
    }

    void render(GuiGraphics g, float x, float y, float w, float h, int mx, int my) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        // Opaque: the rows under it must not show through.
        Draw.rect(g, x, y, x + w, y + h, Theme.PANEL_DARK_HEADER | 0xFF000000);
        Draw.frame(g, x, y, w, h, 1, Theme.PANEL_LINE);
        float fy = y + Grid.GAP;
        field.render(g, x + Grid.GAP, fy, w - 2 * Grid.GAP, mx, my);
        int on = 0;
        for (Entry e : all) if (on(e)) on++;
        if (!field.focused()) {
            DrawText.textRight(g, text("count", on, all.size()), x + w - 2 * Grid.GAP, DrawText.textY(fy, Grid.CHIP_H), Theme.TEXT_MUTED, false);
        }
        float top = listTop(), view = viewH(), s = scroller.update(shown.size() * ITEM_H - view);
        if (shown.isEmpty()) {
            DrawText.text(g, text("none"), x + 2 * Grid.GAP, DrawText.textY(top, ITEM_H), Theme.TEXT_MUTED, false);
        }
        EdgeFade.capture(g);
        g.enableScissor((int) x, (int) top, (int) (x + w), (int) Math.ceil(top + view));
        int first = (int) (s / ITEM_H);
        float ry = top - (s - first * ITEM_H);
        for (int i = first; i < shown.size() && ry < top + view; i++, ry += ITEM_H) {
            Entry e = shown.get(i);
            boolean listed = on(e);
            float ix = x + 2 * Grid.GAP, nameX = ix;
            if (e.e.colour() != 0) {
                Draw.rect(g, ix + 2, ry + 4, ix + 14, ry + 16, e.e.colour());
                nameX = ix + 16 + Grid.GAP;
            }
            float nameW = toggleX() - Grid.GAP - nameX;
            DrawText.text(g, DrawText.fit(e.e.name().getString(), (int) nameW), nameX, DrawText.textY(ry, ITEM_H), listed ? Theme.CREAM : Theme.TEXT_MUTED, false);
            Ui.toggle(g, toggleX(), ry + (ITEM_H - Grid.TOGGLE_H) / 2f, listed, mx, inList(my) ? my : -10_000);
        }
        g.disableScissor();
        EdgeFade.draw(g, x + 1, barX() - Grid.GAP, top, top + view, FADE, Math.min(1, s / FADE),
                Math.min(1, Math.max(0, scroller.max() - s) / FADE));
        EdgeFade.release();
        if (scroller.max() > 0) {
            float bx = barX(), th = thumbH(), ty = thumbY();
            boolean hover = barHit(mx, my);
            if (hover) Cursors.want(Cursors.Shape.RESIZE_V);
            int thumb = hover || dragging ? Theme.GOLD : Theme.SCROLL_THUMB;
            if (Theme.Style.squareUi) {
                Draw.rect(g, bx, top, bx + BAR_W, top + view, Theme.SCROLL_TRACK);
                Draw.rect(g, bx, ty, bx + BAR_W, ty + th, thumb);
            } else {
                Draw.roundRect(g, bx, top, BAR_W, view, BAR_W / 2f, Theme.SCROLL_TRACK);
                Draw.roundRect(g, bx, ty, BAR_W, th, BAR_W / 2f, thumb);
            }
        }
    }

    private float barX() {
        return x + w - 2 * Grid.GAP - BAR_W;
    }

    private float listTop() {
        return y + Grid.GAP + Grid.CHIP_H + Grid.GAP;
    }

    private float viewH() {
        return Math.max(ITEM_H, y + h - 2 * Grid.GAP - listTop());
    }

    private boolean inList(double my) {
        return my >= listTop() && my < listTop() + viewH();
    }

    private float thumbH() {
        float track = viewH();
        return Math.max(Math.min(Grid.ROW, track / 3), track * track / Math.max(1, shown.size() * ITEM_H));
    }

    private float thumbY() {
        return listTop() + (viewH() - thumbH()) * scroller.fraction();
    }

    private boolean barHit(double mx, double my) {
        return scroller.max() > 0 && inList(my) && mx >= barX() - Grid.GAP && mx < barX() + BAR_W + Grid.GAP;
    }

    boolean drag(double my) {
        if (!dragging) return false;
        scroller.toFraction((float) ((my - grab - listTop()) / Math.max(1, viewH() - thumbH())));
        return true;
    }

    void release() {
        dragging = false;
    }

    private float toggleX() {
        return barX() - 2 * Grid.GAP - Grid.TOGGLE_W;
    }

    boolean over(double mx, double my) {
        return Draw.inRect(mx, my, x, y, w, h);
    }

    /** A click on the list: the field, the bar or an entry's switch. True when it was over the list. */
    boolean click(double mx, double my, int button) {
        if (!over(mx, my)) {
            field.blur();
            return false;
        }
        if (field.click(mx, my, button)) return true;
        if (barHit(mx, my)) {
            float ty = thumbY(), th = thumbH();
            grab = my >= ty && my < ty + th ? (float) (my - ty) : th / 2f;
            dragging = true;
            drag(my);
            return true;
        }
        if (!inList(my)) return true;
        int i = (int) ((my - listTop() + scroller.pos()) / ITEM_H);
        float ry = listTop() + i * ITEM_H - scroller.pos();
        if (i < shown.size() && Ui.toggleHit(toggleX(), ry + (ITEM_H - Grid.TOGGLE_H) / 2f, mx, my)) {
            Entry e = shown.get(i);
            set(e, !on(e));
        }
        return true;
    }

    void scrollBy(double amount) {
        scroller.wheel(amount, ITEM_H);
    }

    boolean keyPressed(int key, int scan, int mods) {
        return field.keyPressed(key, scan, mods, null);
    }

    boolean charTyped(char c, int mods) {
        return field.charTyped(c, mods);
    }

    void closed() {
        field.blur();
        if (changed && onClosed != null) onClosed.run();
        changed = false;
    }

    /** A text of the list: the mod's for its row when it has one, else the library's. */
    private Component text(String part, Object... args) {
        String own = rowKey + "." + part;
        return Component.translatable(net.minecraft.client.resources.language.I18n.exists(own) ? own : "aacs.list." + part, args);
    }
}
