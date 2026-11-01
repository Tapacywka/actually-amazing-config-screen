package net.tapacywka.aacs.testmod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.tapacywka.aacs.api.AacsScreen;

/** Development only: the test screen of {@link TestScreen}, opened from the mod list. */
@Mod(value = "aacs_testmod", dist = Dist.CLIENT)
public final class AacsTestMod {
    public AacsTestMod(ModContainer container) {
        AacsScreen screen = TestScreen.make(FMLPaths.CONFIGDIR.get());
        container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> screen.open(parent));
        if (Boolean.getBoolean("aacs.shots")) NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> TestScreen.tick(screen, "neoforge"));
    }
}
