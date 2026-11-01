package net.tapacywka.aacs.testmod;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.tapacywka.aacs.api.AacsScreen;
import net.tapacywka.aacs.api.BoolValue;
import net.tapacywka.aacs.api.ConfigFile;

/** Development only: a settings screen with the rows of AACS, opened from the mod list. */
@Mod(value = "aacs_testmod", dist = net.neoforged.api.distmarker.Dist.CLIENT)
public final class AacsTestMod {
    static BoolValue GRID, LABELS, A, B;
    static final BoolValue[] MORE = new BoolValue[16];

    public AacsTestMod(ModContainer container) {
        ConfigFile.Builder b = ConfigFile.builder();
        b.section("look", s -> {
            GRID = s.bool("grid", true);
            LABELS = s.bool("labels", false);
        });
        b.section("details", s -> {
            A = s.bool("a", true);
            B = s.bool("b", false);
        });
        b.push("more");
        for (int i = 0; i < MORE.length; i++) MORE[i] = b.bool("r" + i, i % 3 == 0);
        b.pop();
        ConfigFile file = b.build();
        file.load(FMLPaths.CONFIGDIR.get().resolve("aacs_testmod-client.json"));

        String k = "aacs_testmod.config.";
        AacsScreen screen = AacsScreen.builder("aacs_testmod", file)
                .tab(k + "tab.display", t -> {
                    t.section(k + "section.look").toggle(GRID).toggle(LABELS);
                    t.subSection(k + "sub.details").toggle(A).toggle(B);
                })
                .tab(k + "tab.extra", t -> {
                    t.section(k + "section.more");
                    for (BoolValue v : MORE) t.toggle(k + "more.n", v);
                })
                .build();
        container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> screen.open(parent));
        if (Boolean.getBoolean("aacs.shots")) Shots.start(screen);
    }
}
