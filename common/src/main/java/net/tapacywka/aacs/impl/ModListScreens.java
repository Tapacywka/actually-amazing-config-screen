package net.tapacywka.aacs.impl;

import net.tapacywka.aacs.api.AacsScreen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The settings screens to show from the mod lists (Mod Menu on Fabric, the mod list of NeoForge): each built screen is
 * added under its mod's id, the first one of a mod staying. The loader's part takes them whenever it is ready (mods are
 * built in any order, on NeoForge on several threads), and a mod's own registration there always wins.
 */
public final class ModListScreens {
    private ModListScreens() {}

    private static final Map<String, AacsScreen> SCREENS = new LinkedHashMap<>();
    private static Consumer<AacsScreen> listener;

    public static synchronized void add(AacsScreen screen) {
        if (SCREENS.putIfAbsent(screen.modId, screen) != null) {
            Mods.LOG.warn("A second settings screen of {}: the mod list keeps the first", screen.modId);
            return;
        }
        if (listener != null) listener.accept(screen);
    }

    /** The screens added so far, by mod id. */
    public static synchronized Map<String, AacsScreen> screens() {
        return Map.copyOf(SCREENS);
    }

    /** Gives every screen added so far to {@code listener}, then each one added later. */
    public static synchronized void listen(Consumer<AacsScreen> listener) {
        ModListScreens.listener = listener;
        SCREENS.values().forEach(listener);
    }
}
