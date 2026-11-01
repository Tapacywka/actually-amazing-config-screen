package net.tapacywka.aacs.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A number from {@link #min()} to {@link #max()}. */
public final class DoubleValue extends Value<Double> {
    private final double min, max;

    DoubleValue(double def, double min, double max) {
        super(def);
        if (!(min <= max) || def < min || def > max) throw new IllegalArgumentException("default " + def + " not in " + min + " to " + max);
        this.min = min;
        this.max = max;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    @Override
    protected Double fix(Double v) {
        return v.isNaN() ? null : Math.max(min, Math.min(max, v));
    }

    @Override
    protected JsonElement write(Double v) {
        return new JsonPrimitive(v);
    }

    @Override
    protected Double read(JsonElement j) {
        return j.isJsonPrimitive() && j.getAsJsonPrimitive().isNumber() ? j.getAsDouble() : null;
    }
}
