package net.tapacywka.aacs.testmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.tapacywka.aacs.api.AacsScreen;

/** Development only: the test screen of {@link TestScreen}, opened from Mod Menu. */
public final class AacsTestModFabric implements ClientModInitializer, ModMenuApi {
    private static AacsScreen screen;

    @Override
    public void onInitializeClient() {
        screen = TestScreen.make(FabricLoader.getInstance().getConfigDir());
        if (Boolean.getBoolean("aacs.shots")) ClientTickEvents.END_CLIENT_TICK.register(mc -> TestScreen.tick(screen, "fabric"));
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> screen.open(parent);
    }
}
