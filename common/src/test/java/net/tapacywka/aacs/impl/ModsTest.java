package net.tapacywka.aacs.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VulkanMod before 0.6.9 keeps a drawn-into texture's top row first; 0.6.9 and its pre-releases as OpenGL does. */
class ModsTest {
    @Test
    void vulkanModBefore069KeepsTheTopRowFirst() {
        assertTrue(Mods.topFirst("0.6.8+26.1.2"));
        assertTrue(Mods.topFirst("0.6.7+1.21.1"));
        assertTrue(Mods.topFirst("0.5.4"));
        assertFalse(Mods.topFirst("0.6.9-dev.5+26.2"), "a pre-release of 0.6.9 already as OpenGL");
        assertFalse(Mods.topFirst("0.6.9+26.2"));
        assertFalse(Mods.topFirst("0.6.10"));
        assertFalse(Mods.topFirst("0.7.0"));
        assertFalse(Mods.topFirst("1.0"));
    }
}
