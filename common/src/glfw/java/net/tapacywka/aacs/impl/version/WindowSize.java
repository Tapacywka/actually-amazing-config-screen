package net.tapacywka.aacs.impl.version;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** The window's size in pixels, GLFW's side (26.1.2 and 26.2; src/sdl has 26.3's); the test mod's narrow window. */
public final class WindowSize {
    private WindowSize() {}

    public static void set(int width, int height) {
        GLFW.glfwSetWindowSize(Minecraft.getInstance().getWindow().handle(), width, height);
    }
}
