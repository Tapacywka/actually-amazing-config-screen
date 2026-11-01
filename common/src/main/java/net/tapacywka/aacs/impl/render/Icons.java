package net.tapacywka.aacs.impl.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.tapacywka.aacs.impl.Mods;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The icons' textures, read once each and kept premultiplied (as {@link Draw#icon} lays them), smoothed when drawn
 * smaller than their 64 pixels.
 */
public final class Icons {
    private Icons() {}

    private static final Map<Icon, DynamicTexture> MADE = new HashMap<>();

    public static int textureId(Icon icon) {
        DynamicTexture t = MADE.computeIfAbsent(icon, Icons::make);
        return t == null ? 0 : t.getId();
    }

    private static DynamicTexture make(Icon icon) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(icon.texture)) {
            NativeImage img = NativeImage.read(in);
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int c = img.getPixelRGBA(x, y), a = (c >>> 24) & 0xFF; // ABGR
                    int b = ((c >> 16) & 0xFF) * a / 255, g = ((c >> 8) & 0xFF) * a / 255, r = (c & 0xFF) * a / 255;
                    img.setPixelRGBA(x, y, a << 24 | b << 16 | g << 8 | r);
                }
            }
            DynamicTexture t = new DynamicTexture(img);
            t.setFilter(true, false);
            return t;
        } catch (Exception e) {
            Mods.LOG.warn("Could not read the icon {}", icon.texture, e);
            return null;
        }
    }
}
