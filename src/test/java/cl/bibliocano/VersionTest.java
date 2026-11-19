package cl.bibliocano;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.bibliocano.consola.DemoConsola;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La versión vive en el {@code pom.xml}; README y CHANGELOG deben repetirla. Un desfase se ve aquí,
 * en el PR, y no al publicar la release.
 */
@DisplayName("Versión y demostración")
class VersionTest {

    private final String version = Version.actual();

    @Test
    @DisplayName("la versión tiene formato SemVer x.y.z")
    void formato() {
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+(-[0-9A-Za-z.]+)?"), version);
    }

    @Test
    @DisplayName("README (badge, tabla y pie) y CHANGELOG dicen la misma versión")
    void documentosSincronizados() throws Exception {
        String readme = Files.readString(Path.of("README.md"), StandardCharsets.UTF_8);
        String changelog = Files.readString(Path.of("CHANGELOG.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("versi%C3%B3n-" + version + "-"), "badge del README");
        assertTrue(readme.contains("| **Versión** | " + version + " |"), "tabla del README");
        assertTrue(readme.contains("BiblioCano v" + version + " ·"), "pie del README");
        assertTrue(changelog.contains("## [" + version + "]"), "sección del CHANGELOG");
    }

    @Test
    @DisplayName("la demo por consola recorre todos los escenarios sin fallos (modo memoria)")
    void demoSinFallos() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int fallos = new DemoConsola(Contexto.memoria(Clock.systemDefaultZone()),
                new PrintStream(buffer, true, StandardCharsets.UTF_8)).ejecutar();

        String salida = buffer.toString(StandardCharsets.UTF_8);
        assertEquals(0, fallos, salida);
        assertTrue(salida.contains("Multa de $3.000"), salida);
    }
}
