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
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
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
            rows.add(new Row(Row.Kind.SECTION, key, null, null, null, null, null, null));
            return this;
        }

        /** A sub-heading that folds: the rows after it, up to the next heading, belong to it. */
        public Tab subSection(String key) {
            rows.add(new Row(Row.Kind.SUB_SECTION, key, null, null, null, null, null, null));
            subSection = key;
            return this;
        }

        /** A sub-heading with its own text (a mod's name); {@code key} still keeps it open or folded. */
        public Tab subSection(String key, Component label) {
            rows.add(new Row(Row.Kind.SUB_SECTION, key, null, null, null, label, null, null));
            subSection = key;
            return this;
        }

        private String keyOf(Value<?> v) {
            return modId + ".config." + v.key();
        }

        private Tab add(Row.Kind kind, String key, Supplier<?> get, Consumer<?> set, Value<?> value, Object options) {
            rows.add(new Row(kind, key, subSection, get, set, null, value, options));
            return this;
        }

        // ---------------------------------------------------------------- switches

        public Tab toggle(BoolValue value) {
            return toggle(keyOf(value), value);
        }

        public Tab toggle(String key, BoolValue value) {
            return add(Row.Kind.TOGGLE, key, value::get, (Boolean b) -> value.set(b), value, null);
        }

        /** A switch over values of the mod's own (several settings at once, say); no reset of its own. */
        public Tab toggle(String key, Supplier<Boolean> get, Consumer<Boolean> set) {
            return add(Row.Kind.TOGGLE, key, get, set, null, null);
        }

        /**
         * A switch with a list under it, opened by an arrow beside the switch while it is on (Teyvat Cartographia's
         * plants): {@code entries} what the list shows, {@code off} the ids switched off, {@code onClosed} run when
         * the list closes after a change (to redraw what it affects), or null.
         */
        public Tab toggleList(BoolValue on, ListValue off, Supplier<List<ListEntry>> entries, Runnable onClosed) {
            return toggleList(keyOf(on), on, off, entries, onClosed);
        }

        public Tab toggleList(String key, BoolValue on, ListValue off, Supplier<List<ListEntry>> entries, Runnable onClosed) {
            return add(Row.Kind.TOGGLE_LIST, key, on::get, (Boolean b) -> on.set(b), on, new ToggleList(off, entries, onClosed));
        }

        // ---------------------------------------------------------------- sliders

        public Tab slider(IntValue value) {
            return slider(keyOf(value), value, s -> {});
        }

        public Tab slider(IntValue value, Consumer<Slider> options) {
            return slider(keyOf(value), value, options);
        }

        public Tab slider(String key, IntValue value, Consumer<Slider> options) {
            Slider s = new Slider(value.min(), value.max(), true);
            options.accept(s);
            return add(Row.Kind.SLIDER, key, () -> (double) value.get(), (Double d) -> value.set((int) Math.round(d)), value, s);
        }

        public Tab slider(DoubleValue value) {
            return slider(keyOf(value), value, s -> {});
        }

        public Tab slider(DoubleValue value, Consumer<Slider> options) {
            return slider(keyOf(value), value, options);
        }

        public Tab slider(String key, DoubleValue value, Consumer<Slider> options) {
            Slider s = new Slider(value.min(), value.max(), false);
            options.accept(s);
            return add(Row.Kind.SLIDER, key, value::get, (Double d) -> value.set(d), value, s);
        }

        /**
         * A choice in steps shown by name ("Off", "Map", "Both maps"): {@code stepKeys} the lang keys of the steps,
         * {@code get} and {@code set} the step (0 first) as the mod keeps it, over one value or several.
         */
        public Tab steps(String key, String[] stepKeys, IntSupplier get, IntConsumer set) {
            Slider s = new Slider(0, stepKeys.length - 1, true);
            s.stepKeys = stepKeys.clone();
            return add(Row.Kind.SLIDER, key, () -> (double) get.getAsInt(), (Double d) -> set.accept((int) Math.round(d)), null, s);
        }

        // ---------------------------------------------------------------- choices with arrows

        /** The constants of an enum by name: {@code <modid>.config.value.<constant in lower case>}. */
        public <E extends Enum<E>> Tab choice(EnumValue<E> value) {
            return choice(keyOf(value), value, c -> {});
        }

        public <E extends Enum<E>> Tab choice(EnumValue<E> value, Consumer<Choice<E>> options) {
            return choice(keyOf(value), value, options);
        }

        public <E extends Enum<E>> Tab choice(String key, EnumValue<E> value, Consumer<Choice<E>> options) {
            String prefix = modId + ".config.value.";
            Choice<E> c = new Choice<>(value::choices, e -> Component.translatable(prefix + e.name().toLowerCase(java.util.Locale.ROOT)));
            options.accept(c);
            return add(Row.Kind.CHOICE, key, value::get, (E e) -> value.set(e), value, c);
        }

        /**
         * A choice among texts the mod lists while the game runs (themes of the resource packs, say), over a
         * {@link StringValue}; {@code names} gives the word for each.
         */
        public Tab choice(String key, StringValue value, Supplier<List<String>> options, Function<String, Component> names) {
            Choice<String> c = new Choice<>(options, names);
            return add(Row.Kind.CHOICE, key, value::get, (String s) -> value.set(s), value, c);
        }
    }

    /** One entry of a {@link Tab#toggleList}: its id (as kept in the list of those off), name and colour (0: none). */
    public record ListEntry(String id, Component name, int colour) {}

    /** What a {@link Tab#toggleList} row opens. */
    public record ToggleList(ListValue off, Supplier<List<ListEntry>> entries, Runnable onClosed) {}

    /** How a slider shows and moves its value. */
    public static final class Slider {
        public final double min, max;
        public final boolean integer;
        public String unit = "";
        public boolean log;
        public double[] steps;
        public String[] stepKeys;
        public DoubleFunction<String> labeler;
        public Runnable onRelease;

        Slider(double min, double max, boolean integer) {
            this.min = min;
            this.max = max;
            this.integer = integer;
        }

        /** Written after the number ("%", " blocks"). */
        public Slider unit(String unit) {
            this.unit = unit;
            return this;
        }

        /** Moves on a logarithmic scale (zoom, distances), for a range over several orders. */
        public Slider log() {
            this.log = true;
            return this;
        }

        /** Snaps to these values only, evenly spaced along the track. */
        public Slider steps(double... steps) {
            this.steps = steps.clone();
            return this;
        }

        /** The value as words of the mod ("Auto" for 0, "1/4" for 0.25). */
        public Slider label(DoubleFunction<String> labeler) {
            this.labeler = labeler;
            return this;
        }

        /** Run once the slider is let go (a costly redraw, say). */
        public Slider onRelease(Runnable onRelease) {
            this.onRelease = onRelease;
            return this;
        }
    }

    /** How a choice with arrows names its options and when it does anything. */
    public static final class Choice<T> {
        public final Supplier<List<T>> options;
        public Function<T, Component> names;
        public BooleanSupplier activeWhen = () -> true;

        Choice(Supplier<List<T>> options, Function<T, Component> names) {
            this.options = options;
            this.names = names;
        }

        public Choice<T> names(Function<T, Component> names) {
            this.names = names;
            return this;
        }

        /** Dimmed, and a click does nothing, while this is false (a format of a clock that is off). */
        public Choice<T> activeWhen(BooleanSupplier activeWhen) {
            this.activeWhen = activeWhen;
            return this;
        }
    }

    /**
     * A row as the builder made it; the screen draws it. {@code value} is the setting behind it (its reset icon), null
     * for rows over the mod's own getter and setter; {@code options} a {@link Slider} or a {@link Choice}.
     */
    public record Row(Kind kind, String key, String subSection, Supplier<?> get, Consumer<?> set, Component label,
                      Value<?> value, Object options) {
        public enum Kind { SECTION, SUB_SECTION, TOGGLE, TOGGLE_LIST, SLIDER, CHOICE }
    }
}
