package net.tapacywka.aacs.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/** One of the constants of an enum (or of some of them), written by name; the case in the file does not matter. */
public final class EnumValue<E extends Enum<E>> extends Value<E> {
    private final List<E> choices;

    EnumValue(E def, List<E> choices) {
        super(def);
        if (!choices.contains(def)) throw new IllegalArgumentException("default " + def + " not among " + choices);
        this.choices = List.copyOf(choices);
    }

    /** The constants it may hold, in order. */
    public List<E> choices() {
        return choices;
    }

    @Override
    protected E fix(E v) {
        return choices.contains(v) ? v : null;
    }

    @Override
    protected JsonElement write(E v) {
        return new JsonPrimitive(v.name());
    }

    @Override
    protected E read(JsonElement j) {
        if (!j.isJsonPrimitive() || !j.getAsJsonPrimitive().isString()) return null;
        for (E e : getDefault().getDeclaringClass().getEnumConstants()) {
            if (e.name().equalsIgnoreCase(j.getAsString())) return e;
        }
        return null;
    }
}
