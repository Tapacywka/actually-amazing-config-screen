package net.tapacywka.aacs.api;

/**
 * Actually Amazing Config Screen: the entry point of the public API. Everything a mod may use lives in this package;
 * {@code net.tapacywka.aacs.impl} is internal and may change in any version.
 */
public final class Aacs {
    private Aacs() {}

    public static final String MOD_ID = "aacs";

    /**
     * The version of the public API, raised with every addition. The loader keeps only the newest copy of the library
     * embedded in the mods of a game, so a newer one keeps every older method working; a mod built against version
     * {@code n} may check {@code API_VERSION >= n} before using what came with it.
     */
    public static final int API_VERSION = 2;
}
