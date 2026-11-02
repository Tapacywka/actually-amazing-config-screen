package net.tapacywka.aacs.testmod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Development only: the test screen of {@link TestScreen}; its button in the mod list comes from the library. */
@Mod(value = "aacs_testmod", dist = Dist.CLIENT)
public final class AacsTestMod {
    public AacsTestMod(ModContainer container) {
        TestScreen.make(FMLPaths.CONFIGDIR.get());
        // The shots open the screen as the mod list's button does.
        if (Boolean.getBoolean("aacs.shots")) {
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> TestScreen.tick(parent -> IConfigScreenFactory.getForMod(container.getModInfo())
                    .map(f -> f.createScreen(container, parent)).orElse(null), "neoforge"));
        }
    }
}
