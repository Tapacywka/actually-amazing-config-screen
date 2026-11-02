package net.tapacywka.aacs.impl.version;

import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLVideo;

/** The window's size in pixels, SDL3's side (26.3; src/glfw has the GLFW games'); the test mod's narrow window. */
public final class WindowSize {
    private WindowSize() {}

    public static void set(int width, int height) {
        SDLVideo.SDL_SetWindowSize(Minecraft.getInstance().getWindow().handle(), width, height);
    }
}
