package net.tapacywka.aacs.impl.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.tapacywka.aacs.impl.Mods;
import net.tapacywka.aacs.impl.version.Game;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The icons' textures, read once each and kept premultiplied (as {@link Draw#icon} lays them), smoothed when drawn
 * smaller than their 64 pixels.
 */
public final class Icons {
    private Icons() {}

    private static final Map<Icon, GpuImage> MADE = new HashMap<>();

    /** The icon's texture; null if it could not be read. */
    public static Tex tex(Icon icon) {
        GpuImage t = MADE.computeIfAbsent(icon, Icons::make);
        return t == null ? null : t.tex(true);
    }

    private static GpuImage make(Icon icon) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(icon.texture);
             NativeImage img = Game.readImage(in)) {
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int c = img.getPixel(x, y), a = c >>> 24; // ARGB
                    int r = ((c >> 16) & 0xFF) * a / 255, g = ((c >> 8) & 0xFF) * a / 255, b = (c & 0xFF) * a / 255;
                    img.setPixel(x, y, a << 24 | r << 16 | g << 8 | b);
                }
            }
            return new GpuImage("AACS icon " + icon.texture, img);
        } catch (Exception e) {
            Mods.LOG.warn("Could not read the icon {}", icon.texture, e);
            return null;
        }
    }
}
