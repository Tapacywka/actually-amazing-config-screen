package net.tapacywka.aacs.api;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tapacywka.aacs.impl.gui.ConfigScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A mod's settings screen, laid out over the {@link Value}s of its {@link ConfigFile}: tabs, sections, sub-sections
 * that fold, and the rows. Made once by {@link #builder}; {@link #open} gives a new screen each time.
 * <p>
 * Texts come from the mod's lang files: the title {@code <modid>.config.title}, a tab, section or sub-section its key,
 * a row {@code <modid>.config.<value key>} (or the key given) and its description that key plus {@code .tooltip}.
 */
public final class AacsScreen {
    /** Draws the screen's background (under the panel), {@code width} x {@code height} GUI units. */
    @FunctionalInterface
    public interface Background {
        void draw(GuiGraphics g, int width, int height, float partialTick);
    }

    /** A button of the mod left of Reset and Done; {@code screen} is the settings screen (to open another over it). */
    public record Button(String key, ResourceLocation icon, Consumer<Screen> onClick) {}

    public final String modId;
    public final ConfigFile file;
    public final Component title;
    public final ResourceLocation icon;
    public final Palette palette;
    public final BooleanSupplier rounded;
    public final Background background;
    public final List<Button> buttons;
    public final Runnable onReset;
    public final List<Tab> tabs;

    private AacsScreen(Builder b) {
        modId = b.modId;
        file = b.file;
        title = b.title != null ? b.title : Component.translatable(modId + ".config.title");
        icon = b.icon;
        palette = b.palette;
        rounded = b.rounded;
        background = b.background;
        buttons = List.copyOf(b.buttons);
        onReset = b.onReset;
        tabs = List.copyOf(b.tabs);
    }

    public static Builder builder(String modId, ConfigFile file) {
        return new Builder(modId, file);
    }

    /** A new settings screen; closing it saves the file and goes back to {@code parent}. */
    public Screen open(Screen parent) {
        return new ConfigScreen(this, parent);
    }

    public static final class Builder {
        private final String modId;
        private final ConfigFile file;
        private Component title;
        private ResourceLocation icon;
        private Palette palette = Palette.DEFAULT;
        private BooleanSupplier rounded = () -> false;
        private Background background;
        private final List<Button> buttons = new ArrayList<>();
        private Runnable onReset = () -> {};
        private final List<Tab> tabs = new ArrayList<>();

        private Builder(String modId, ConfigFile file) {
            this.modId = modId;
            this.file = file;
        }

        public Builder title(Component title) {
            this.title = title;
            return this;
        }

        /** A white 64 x 64 texture shown left of the title (tinted gold); none by default. */
        public Builder icon(ResourceLocation icon) {
            this.icon = icon;
            return this;
        }

        public Builder palette(Palette palette) {
            this.palette = palette;
            return this;
        }

        /** The rounded shapes while this says so (a setting of the mod), else the rectangular ones. */
        public Builder rounded(BooleanSupplier rounded) {
            this.rounded = rounded;
            return this;
        }

        /** The mod's own background; without one, the game's (the world blurred, or its menu background). */
        public Builder background(Background background) {
            this.background = background;
            return this;
        }

        public Builder button(String key, ResourceLocation icon, Consumer<Screen> onClick) {
            buttons.add(new Button(key, icon, onClick));
            return this;
        }

        /** Run after Reset has set every value back to its default (to redraw what they show). */
        public Builder onReset(Runnable onReset) {
            this.onReset = onReset;
            return this;
        }

        public Builder tab(String key, Consumer<Tab> body) {
            Tab t = new Tab(modId, key);
            body.accept(t);
            tabs.add(t);
            return this;
        }

        public AacsScreen build() {
            if (tabs.isEmpty()) throw new IllegalStateException("a settings screen without tabs");
            return new AacsScreen(this);
        }
    }

    /** One tab: its rows in order. A tab strip is shown only when there are two tabs or more. */
    public static final class Tab {
        public final String key;
        private final String modId;
        public final List<Row> rows = new ArrayList<>();
        private String subSection;

        private Tab(String modId, String key) {
            this.modId = modId;
            this.key = key;
        }

        /** A heading; it also ends the sub-section before it. */
        public Tab section(String key) {
            subSection = null;
            rows.add(new Row(Row.Kind.SECTION, key, null, null, null, null));
            return this;
        }

        /** A sub-heading that folds: the rows after it, up to the next heading, belong to it. */
        public Tab subSection(String key) {
            rows.add(new Row(Row.Kind.SUB_SECTION, key, null, null, null, null));
            subSection = key;
            return this;
        }

        /** A sub-heading with its own text (a mod's name); {@code key} still keeps it open or folded. */
        public Tab subSection(String key, Component label) {
            rows.add(new Row(Row.Kind.SUB_SECTION, key, null, null, null, label));
            subSection = key;
            return this;
        }

        public Tab toggle(BoolValue value) {
            return toggle(modId + ".config." + value.key(), value);
        }

        public Tab toggle(String key, BoolValue value) {
            return toggle(key, value::get, value::set);
        }

        /** A switch over values of the mod's own (several settings at once, say). */
        public Tab toggle(String key, Supplier<Boolean> get, Consumer<Boolean> set) {
            rows.add(new Row(Row.Kind.TOGGLE, key, subSection, get, set, null));
            return this;
        }
    }

    /** A row as the builder made it; the screen draws it. */
    public record Row(Kind kind, String key, String subSection, Supplier<?> get, Consumer<?> set, Component label) {
        public enum Kind { SECTION, SUB_SECTION, TOGGLE }
    }
}
