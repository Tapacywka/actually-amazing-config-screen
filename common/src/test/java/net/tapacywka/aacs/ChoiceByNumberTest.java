package net.tapacywka.aacs;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.tapacywka.aacs.api.AacsScreen;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A choice with arrows among named options over the mod's own number (a clock: off, game, real, both). */
class ChoiceByNumberTest {
    @Test
    @SuppressWarnings("unchecked")
    void stepsThroughTheNamedOptionsByNumber() {
        int[] kept = {2};
        boolean[] on = {true};
        String[] keys = {"t.off", "t.game", "t.real", "t.both"};
        AacsScreen.Tab[] tab = new AacsScreen.Tab[1];
        AacsScreen.builder("t", null).tab("t.tab", t -> {
            tab[0] = t;
            t.choice("t.clock", keys, () -> kept[0], i -> kept[0] = i, c -> c.activeWhen(() -> on[0]));
        });
        // The caller's array may change afterwards: the row keeps its own copy.
        keys[0] = "changed";

        AacsScreen.Row row = tab[0].rows.get(0);
        assertEquals(AacsScreen.Row.Kind.CHOICE, row.kind());
        assertEquals("t.clock", row.key());
        assertNull(row.value(), "no single value behind it, so no reset icon");

        AacsScreen.Choice<Integer> c = (AacsScreen.Choice<Integer>) row.options();
        assertEquals(List.of(0, 1, 2, 3), c.options.get());
        assertEquals("t.off", key(c.names.apply(0)));
        assertEquals("t.both", key(c.names.apply(3)));

        assertEquals(2, ((Supplier<Integer>) row.get()).get());
        ((Consumer<Integer>) row.set()).accept(3);
        assertEquals(3, kept[0]);

        assertTrue(c.activeWhen.getAsBoolean());
        on[0] = false;
        assertFalse(c.activeWhen.getAsBoolean());
    }

    private static String key(Component c) {
        return ((TranslatableContents) c.getContents()).getKey();
    }
}
