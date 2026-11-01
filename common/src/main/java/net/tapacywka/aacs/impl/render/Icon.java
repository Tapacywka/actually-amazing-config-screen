package net.tapacywka.aacs.impl.render;

import net.minecraft.resources.ResourceLocation;

/**
 * The library's own white glyphs ({@code assets/aacs/textures/icon/<name>.png}, 64 x 64, from Nieobie's Game Icon Pack,
 * CC0, as drawn for Teyvat Cartographia), tinted when drawn. A mod's own icon is any texture of the same kind.
 */
public final class Icon {
    public static final Icon GEAR = new Icon(Theme.asset("textures/icon/gear.png"));
    public static final Icon CHECK = new Icon(Theme.asset("textures/icon/check.png"));
    public static final Icon RESET = new Icon(Theme.asset("textures/icon/reset.png"));
    public static final Icon CLOSE = new Icon(Theme.asset("textures/icon/close.png"));

    public final ResourceLocation texture;

    public Icon(ResourceLocation texture) {
        this.texture = texture;
    }
}
