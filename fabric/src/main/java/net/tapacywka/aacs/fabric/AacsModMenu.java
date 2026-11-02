package net.tapacywka.aacs.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.tapacywka.aacs.impl.ModListScreens;

import java.util.HashMap;
import java.util.Map;

/**
 * The settings buttons of Mod Menu for every mod with a settings screen of the library (loaded by Mod Menu only, so
 * the library does not need it). Mod Menu asks again each time it looks a screen up, and a mod's own entry point wins.
 */
public final class AacsModMenu implements ModMenuApi {
    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> factories = new HashMap<>();
        ModListScreens.screens().forEach((id, screen) -> factories.put(id, screen::open));
        return factories;
    }
}
