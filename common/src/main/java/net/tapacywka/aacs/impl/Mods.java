package net.tapacywka.aacs.impl;

/**
 * Which other mods are loaded, asked of whichever loader runs the game (common code knows neither loader's API, so
 * both are asked by reflection, once each).
 */
public final class Mods {
    private Mods() {}

    public static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(net.tapacywka.aacs.api.Aacs.MOD_ID);

    /** The name of a mod as its loader shows it, or its id. */
    public static String name(String id) {
        try {
            Object loader = Class.forName("net.fabricmc.loader.api.FabricLoader").getMethod("getInstance").invoke(null);
            Object c = ((java.util.Optional<?>) Class.forName("net.fabricmc.loader.api.FabricLoader").getMethod("getModContainer", String.class).invoke(loader, id)).orElse(null);
            if (c != null) {
                Object meta = Class.forName("net.fabricmc.loader.api.ModContainer").getMethod("getMetadata").invoke(c);
                return (String) Class.forName("net.fabricmc.loader.api.metadata.ModMetadata").getMethod("getName").invoke(meta);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Not Fabric.
        }
        try {
            Object list = Class.forName("net.neoforged.fml.ModList").getMethod("get").invoke(null);
            Object c = ((java.util.Optional<?>) list.getClass().getMethod("getModContainerById", String.class).invoke(list, id)).orElse(null);
            if (c != null) {
                Object info = Class.forName("net.neoforged.fml.ModContainer").getMethod("getModInfo").invoke(c);
                return (String) Class.forName("net.neoforged.neoforgespi.language.IModInfo").getMethod("getDisplayName").invoke(info);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Not NeoForge either.
        }
        return id;
    }

    public static boolean loaded(String id) {
        try {
            Object loader = Class.forName("net.fabricmc.loader.api.FabricLoader").getMethod("getInstance").invoke(null);
            return (boolean) loader.getClass().getMethod("isModLoaded", String.class).invoke(loader, id);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Not Fabric.
        }
        try {
            Object list = Class.forName("net.neoforged.fml.ModList").getMethod("get").invoke(null);
            return (boolean) list.getClass().getMethod("isLoaded", String.class).invoke(list, id);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static int vulkan, topFirst;

    /** VulkanMod (or VulkanMod-Reforged, the same id) draws the game. */
    public static boolean vulkan() {
        if (vulkan == 0) vulkan = loaded("vulkanmod") ? 1 : -1;
        return vulkan > 0;
    }

    /**
     * VulkanMod before 0.6.9 keeps a texture drawn into with its top row first, OpenGL and later VulkanMod the bottom
     * row first (lesson of Teyvat Cartographia, 30.10.2026: 0.6.8 for 26.1.2 so, 0.6.9-dev.5 for 26.2 as OpenGL).
     */
    private static boolean vulkanTopFirst() {
        return vulkan() && topFirst(version("vulkanmod"));
    }

    /** A version is before 0.6.9 (its pre-releases 0.6.9-dev.N already count as 0.6.9). */
    static boolean topFirst(String version) {
        String[] parts = version.split("[^0-9]+", 4);
        int[] want = {0, 6, 9};
        for (int i = 0; i < want.length; i++) {
            int n = i < parts.length && !parts[i].isEmpty() ? Integer.parseInt(parts[i]) : 0;
            if (n != want[i]) return n < want[i];
        }
        return false;
    }

    /** The version of a mod as its loader has it, or "" when it is not loaded. */
    public static String version(String id) {
        try {
            Object loader = Class.forName("net.fabricmc.loader.api.FabricLoader").getMethod("getInstance").invoke(null);
            Object c = ((java.util.Optional<?>) Class.forName("net.fabricmc.loader.api.FabricLoader").getMethod("getModContainer", String.class).invoke(loader, id)).orElse(null);
            if (c == null) return "";
            Object meta = Class.forName("net.fabricmc.loader.api.ModContainer").getMethod("getMetadata").invoke(c);
            Object v = Class.forName("net.fabricmc.loader.api.metadata.ModMetadata").getMethod("getVersion").invoke(meta);
            return (String) Class.forName("net.fabricmc.loader.api.Version").getMethod("getFriendlyString").invoke(v);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Not Fabric.
        }
        try {
            Object list = Class.forName("net.neoforged.fml.ModList").getMethod("get").invoke(null);
            Object c = ((java.util.Optional<?>) list.getClass().getMethod("getModContainerById", String.class).invoke(list, id)).orElse(null);
            if (c == null) return "";
            Object info = Class.forName("net.neoforged.fml.ModContainer").getMethod("getModInfo").invoke(c);
            return String.valueOf(Class.forName("net.neoforged.neoforgespi.language.IModInfo").getMethod("getVersion").invoke(info));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return "";
        }
    }

    /** A texture coordinate {@code v} of a layer the library drew into, as the renderer keeps its rows. */
    public static float v(float v) {
        if (topFirst == 0) topFirst = vulkanTopFirst() ? 1 : -1;
        return topFirst > 0 ? 1 - v : v;
    }
}
