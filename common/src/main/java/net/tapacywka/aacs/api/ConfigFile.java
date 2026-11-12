package net.tapacywka.aacs.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A mod's settings file: JSON, the sections as nested objects ({@code { "minimap": { "radius": 48 } }}), the same
 * format as the settings of Teyvat Cartographia. Made by {@link #builder()}, which hands out the {@link Value}s the
 * mod keeps.
 * <p>
 * Reading forgives: a number out of range is brought into it, a value of the wrong kind or an unknown choice becomes
 * the default (one line in the log names them), and keys the mod does not know stay in the file, so going back to an
 * older version of the mod loses nothing.
 */
public final class ConfigFile {
    private static final Logger LOG = LoggerFactory.getLogger(Aacs.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Map<String, Value<?>> values;
    private Path file;
    /** The file as last read: its unknown keys are written back with the values. */
    private JsonObject read = new JsonObject();
    private int generation;

    private ConfigFile(Map<String, Value<?>> values) {
        this.values = values;
        for (Value<?> v : values.values()) v.file = this;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Every value, in the order they were made. */
    public Collection<Value<?>> values() {
        return Collections.unmodifiableCollection(values.values());
    }

    /** Goes up with every change of a value: a cache keyed by it knows when to redraw. */
    public int generation() {
        return generation;
    }

    void changed() {
        generation++;
    }

    /** Where the file is read from and saved to; null before {@link #load}. */
    public Path path() {
        return file;
    }

    /**
     * Reads the file, from now on saved there, and writes it back at once (with the values it lacked). Returns whether
     * it existed: a mod taking over an older settings file of its own does that when it did not, then saves.
     */
    public boolean load(Path file) {
        this.file = file;
        boolean existed = Files.exists(file);
        if (existed) {
            try {
                JsonElement root = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonElement.class);
                read = root != null && root.isJsonObject() ? root.getAsJsonObject() : new JsonObject();
                List<String> fixed = new ArrayList<>();
                for (Value<?> v : values.values()) {
                    JsonElement j = find(read, v.path);
                    if (j != null && v.load(j)) fixed.add(v.key());
                }
                if (!fixed.isEmpty()) LOG.warn("Settings file {}: {} out of range or of the wrong kind, fixed", file.getFileName(), fixed);
            } catch (IOException | RuntimeException ex) {
                // Kept beside it as it was (a player may mend it by hand) before the defaults are written over it.
                Path broken = file.resolveSibling(file.getFileName() + ".broken");
                LOG.warn("The settings file {} could not be read; the defaults are used, the file kept as {}", file, broken.getFileName(), ex);
                try {
                    Files.copy(file, broken, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                    // Nothing more to keep then.
                }
                read = new JsonObject();
            }
        }
        save();
        return existed;
    }

    /** Writes the file (through a temporary one, so a crash midway never leaves half a file). */
    public void save() {
        if (file == null) return;
        JsonObject root = read.deepCopy();
        for (Value<?> v : values.values()) {
            JsonObject o = root;
            for (int i = 0; i < v.path.size() - 1; i++) {
                JsonElement next = o.get(v.path.get(i));
                if (next == null || !next.isJsonObject()) {
                    next = new JsonObject();
                    o.add(v.path.get(i), next);
                }
                o = next.getAsJsonObject();
            }
            o.add(v.path.get(v.path.size() - 1), v.write());
        }
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // A file system without atomic moves (some network drives): replaced in two steps there.
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            read = root;
        } catch (IOException ex) {
            LOG.warn("The settings could not be saved to {}", file, ex);
        }
    }

    private static JsonElement find(JsonObject root, List<String> path) {
        JsonElement j = root;
        for (String part : path) {
            if (j == null || !j.isJsonObject()) return null;
            j = j.getAsJsonObject().get(part);
        }
        return j;
    }

    /** Makes the values section by section: {@code push("minimap")}, the values, {@code pop()}; or {@link #section}. */
    public static final class Builder {
        private final Deque<String> path = new ArrayDeque<>();
        private final Map<String, Value<?>> values = new LinkedHashMap<>();

        private Builder() {}

        public Builder push(String section) {
            path.addLast(name(section));
            return this;
        }

        public Builder pop() {
            if (path.isEmpty()) throw new IllegalStateException("pop() without push()");
            path.removeLast();
            return this;
        }

        /** The values made in {@code body} belong to the section {@code name}. */
        public Builder section(String name, Consumer<Builder> body) {
            push(name);
            body.accept(this);
            return pop();
        }

        public BoolValue bool(String name, boolean def) {
            return add(name, new BoolValue(def));
        }

        public IntValue integer(String name, int def, int min, int max) {
            return add(name, new IntValue(def, min, max));
        }

        public DoubleValue decimal(String name, double def, double min, double max) {
            return add(name, new DoubleValue(def, min, max));
        }

        public <E extends Enum<E>> EnumValue<E> choice(String name, E def) {
            return add(name, new EnumValue<>(def, Arrays.asList(def.getDeclaringClass().getEnumConstants())));
        }

        public <E extends Enum<E>> EnumValue<E> choice(String name, E def, List<E> choices) {
            return add(name, new EnumValue<>(def, choices));
        }

        public StringValue text(String name, String def, Predicate<String> valid) {
            return add(name, new StringValue(def, valid));
        }

        public ListValue list(String name, List<String> def, Predicate<String> entry) {
            return add(name, new ListValue(def, entry));
        }

        public ConfigFile build() {
            if (!path.isEmpty()) throw new IllegalStateException("push() without pop(): " + path);
            return new ConfigFile(values);
        }

        private <V extends Value<?>> V add(String name, V v) {
            List<String> full = new ArrayList<>(path);
            full.add(name(name));
            String key = String.join(".", full);
            for (String other : values.keySet()) {
                // A value and a section of the same name would be one key of the file.
                if (other.equals(key) || other.startsWith(key + ".") || key.startsWith(other + ".")) {
                    throw new IllegalArgumentException(key + " clashes with " + other);
                }
            }
            v.path = List.copyOf(full);
            values.put(key, v);
            return v;
        }

        private static String name(String n) {
            if (n.isEmpty() || n.contains(".")) throw new IllegalArgumentException("a name without dots: \"" + n + "\"");
            return n;
        }
    }
}
