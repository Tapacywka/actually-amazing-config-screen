package net.tapacywka.aacs.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A whole number from {@link #min()} to {@link #max()}. */
public final class IntValue extends Value<Integer> {
    private final int min, max;

    IntValue(int def, int min, int max) {
        super(def);
        if (min > max || def < min || def > max) throw new IllegalArgumentException("default " + def + " not in " + min + " to " + max);
        this.min = min;
        this.max = max;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    @Override
    protected Integer fix(Integer v) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    protected JsonElement write(Integer v) {
        return new JsonPrimitive(v);
    }

    /** A whole number of the file; one with a fraction is of the wrong kind. */
    @Override
    protected Integer read(JsonElement j) {
        if (!j.isJsonPrimitive() || !j.getAsJsonPrimitive().isNumber()) return null;
        double d = j.getAsDouble();
        if (Double.isNaN(d)) return null;
        long r = Math.round(d);
        if (r != d) return null;
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, r));
    }
}
