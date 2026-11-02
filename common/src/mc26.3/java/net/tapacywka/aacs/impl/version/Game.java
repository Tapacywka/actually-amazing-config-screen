package net.tapacywka.aacs.impl.version;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

import java.io.IOException;
import java.io.InputStream;

/** The parts of the game that moved between the 26.x games: Minecraft 26.3's side (mc26.1.2 and mc26.2 have the others). */
public final class Game {
    private Game() {}

    public static Screen screen() {
        return Minecraft.getInstance().gui.screen();
    }

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    /** A PNG read as RGBA. */
    public static NativeImage readImage(InputStream in) throws IOException {
        return NativeImage.read(in); // 26.3 reads every PNG as RGBA;
    }

    /** The key's second code: its scancode (26.3: its layout's keycode). */
    public static int scancode(KeyEvent event) {
        return event.keycode();
    }

    /** The frame the game draws into (the test mod's shots). */
    public static RenderTarget mainTarget() {
        return Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }
}
