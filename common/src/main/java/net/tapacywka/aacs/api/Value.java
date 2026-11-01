package net.tapacywka.aacs.api;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One setting of a {@link ConfigFile}, made by its builder: the mod keeps the reference and reads it with
 * {@link #get()}. A change acts at once (the game sees it live); the file is written when the settings screen closes
 * or when the mod calls {@link ConfigFile#save()}. Values are read and changed on the game's thread only.
 */
public abstract class Value<T> implements Supplier<T> {
    /** Told of a change on the game's thread, at once, with the value before and after it. */
    @FunctionalInterface
    public interface Listener<T> {
        void changed(T before, T after);
    }

    private final T def;
    private T value;
    private final List<Listener<T>> listeners = new ArrayList<>(1);
    List<String> path = List.of();
    ConfigFile file;

    protected Value(T def) {
        this.def = def;
        this.value = def;
    }

    @Override
    public T get() {
        return value;
    }

    public T getDefault() {
        return def;
    }

    public boolean isDefault() {
        return value.equals(def);
    }

    /** The section names and the value's own name, as in the file. */
    public List<String> path() {
        return path;
    }

    /** The path joined by dots ("minimap.radius"). */
    public String key() {
        return String.join(".", path);
    }

    /**
     * Sets the value. A number out of its range is brought into it; a value this setting cannot hold at all throws
     * {@link IllegalArgumentException}.
     */
    public void set(T v) {
        T fixed = v == null ? null : fix(v);
        if (fixed == null) throw new IllegalArgumentException(key() + " cannot be " + v);
        change(fixed);
    }

    public void reset() {
        change(def);
    }

    public void onChange(Listener<T> listener) {
        listeners.add(listener);
    }

    private void change(T now) {
        if (now.equals(value)) return;
        T before = value;
        value = now;
        if (file != null) file.changed();
        for (Listener<T> l : listeners) l.changed(before, now);
    }

    /** {@code v} as this setting holds it (a number brought into its range), or null when it cannot. */
    protected abstract T fix(T v);

    /** The value as written in the file. */
    protected abstract JsonElement write(T v);

    /** A value from the file, or null when it is of the wrong kind (the caller then uses the default). */
    protected abstract T read(JsonElement j);

    /** Reads from the file: true when the value had to be changed on the way (out of range, wrong kind). */
    boolean load(JsonElement j) {
        T parsed = read(j);
        T fixed = parsed == null ? null : fix(parsed);
        change(fixed == null ? def : fixed);
        return fixed == null || !fixed.equals(parsed);
    }

    JsonElement write() {
        return write(value);
    }
}
