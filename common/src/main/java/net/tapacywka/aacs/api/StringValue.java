package net.tapacywka.aacs.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.Predicate;

/** A text the mod checks itself (an id, a name): one it does not accept is not taken. */
public final class StringValue extends Value<String> {
    private final Predicate<String> valid;

    StringValue(String def, Predicate<String> valid) {
        super(def);
        if (!valid.test(def)) throw new IllegalArgumentException("default \"" + def + "\" not valid");
        this.valid = valid;
    }

    @Override
    protected String fix(String v) {
        return valid.test(v) ? v : null;
    }

    @Override
    protected JsonElement write(String v) {
        return new JsonPrimitive(v);
    }

    @Override
    protected String read(JsonElement j) {
        return j.isJsonPrimitive() && j.getAsJsonPrimitive().isString() ? j.getAsString() : null;
    }
}
