package net.tapacywka.aacs.testmod;

import com.terraformersmc.modmenu.ModMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

/** Development only: the test screen of {@link TestScreen}; its button in Mod Menu comes from the library. */
public final class AacsTestModFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TestScreen.make(FabricLoader.getInstance().getConfigDir());
        // The shots open the screen as Mod Menu's button does.
        if (Boolean.getBoolean("aacs.shots")) {
            ClientTickEvents.END_CLIENT_TICK.register(mc -> TestScreen.tick(parent -> ModMenu.getConfigScreen("aacs_testmod", parent), "fabric"));
        }
    }
}
