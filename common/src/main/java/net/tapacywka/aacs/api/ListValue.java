package net.tapacywka.aacs.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A list of texts (block ids, names), never changed in place: {@link #set} takes a new list. An entry the mod does
 * not accept is left out, the rest kept.
 */
public final class ListValue extends Value<List<String>> {
    private final Predicate<String> entry;

    ListValue(List<String> def, Predicate<String> entry) {
        super(List.copyOf(def));
        if (!def.stream().allMatch(entry)) throw new IllegalArgumentException("default " + def + " has an entry not valid");
        this.entry = entry;
    }

    @Override
    protected List<String> fix(List<String> v) {
        List<String> kept = new ArrayList<>(v.size());
        for (String s : v) if (s != null && entry.test(s)) kept.add(s);
        return List.copyOf(kept);
    }

    @Override
    protected JsonElement write(List<String> v) {
        JsonArray a = new JsonArray();
        for (String s : v) a.add(s);
        return a;
    }

    /** The texts of the array; an entry of another kind counts as one not accepted. */
    @Override
    protected List<String> read(JsonElement j) {
        if (!j.isJsonArray()) return null;
        List<String> l = new ArrayList<>();
        for (JsonElement e : j.getAsJsonArray()) {
            l.add(e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null);
        }
        return l;
    }
}
