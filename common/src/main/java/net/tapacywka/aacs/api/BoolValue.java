package net.tapacywka.aacs.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** On or off. */
public final class BoolValue extends Value<Boolean> {
    BoolValue(boolean def) {
        super(def);
    }

    @Override
    protected Boolean fix(Boolean v) {
        return v;
    }

    @Override
    protected JsonElement write(Boolean v) {
        return new JsonPrimitive(v);
    }

    @Override
    protected Boolean read(JsonElement j) {
        return j.isJsonPrimitive() && j.getAsJsonPrimitive().isBoolean() ? j.getAsBoolean() : null;
    }
}
