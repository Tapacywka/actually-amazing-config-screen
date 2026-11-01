package net.tapacywka.aacs;

import net.tapacywka.aacs.api.Aacs;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AacsTest {
    @Test
    void modIdMatchesTheBuild() throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(Path.of(System.getProperty("aacs.projectDir"), "..", "gradle.properties"))) {
            p.load(r);
        }
        assertEquals(p.getProperty("mod_id"), Aacs.MOD_ID);
        assertTrue(Aacs.API_VERSION >= 1);
    }
}
