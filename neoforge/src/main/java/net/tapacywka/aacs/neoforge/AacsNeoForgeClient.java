package net.tapacywka.aacs.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.tapacywka.aacs.api.Aacs;
import net.tapacywka.aacs.impl.ModListScreens;

/**
 * The library on the NeoForge client: the settings button in the mod list for every mod with a settings screen of the
 * library, unless the mod registered a screen there itself.
 */
@Mod(value = Aacs.MOD_ID, dist = Dist.CLIENT)
public final class AacsNeoForgeClient {
    public AacsNeoForgeClient() {
        ModListScreens.listen(screen -> ModList.get().getModContainerById(screen.modId).ifPresent(c -> {
            if (c.getCustomExtension(IConfigScreenFactory.class).isEmpty()) {
                c.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> screen.open(parent));
            }
        }));
    }
}
