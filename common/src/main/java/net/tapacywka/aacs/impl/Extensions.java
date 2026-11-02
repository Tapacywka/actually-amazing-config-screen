package net.tapacywka.aacs.impl;

import net.tapacywka.aacs.api.AacsScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * The sub-sections mods add to the tabs other mods open to them ({@link AacsScreen#extend}), read each time a screen
 * opens: the order mods load in does not matter.
 */
public final class Extensions {
    private Extensions() {}

    private static final List<AacsScreen.Extension> ALL = new ArrayList<>();
    private static final java.util.Set<AacsScreen.Extension> WARNED = new java.util.HashSet<>();

    public static synchronized void add(AacsScreen.Extension e) {
        ALL.add(e);
    }

    /** The sub-sections added to the tab of {@code owner} opened as {@code tab}. */
    public static synchronized List<AacsScreen.Extension> of(String owner, String tab) {
        List<AacsScreen.Extension> out = new ArrayList<>();
        for (AacsScreen.Extension e : ALL) if (e.owner.equals(owner) && e.tab.equals(tab)) out.add(e);
        return out;
    }

    /** Sub-sections added to a tab their owner's screen does not open (or to no such screen): logged once each. */
    public static synchronized void checkAgainst(AacsScreen screen) {
        List<String> open = new ArrayList<>();
        for (AacsScreen.Tab t : screen.tabs) if (t.openTo() != null) open.add(t.openTo());
        for (AacsScreen.Extension e : ALL) {
            if (e.owner.equals(screen.modId) && !open.contains(e.tab) && WARNED.add(e)) {
                Mods.LOG.warn("{} adds to the tab \"{}\" of {}, which is not open to other mods: not shown", e.modId, e.tab, e.owner);
            }
        }
    }
}
