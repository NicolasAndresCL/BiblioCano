package cl.bibliocano;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Versión del sistema. Sale del {@code pom.xml} (Maven la escribe en {@code version.properties} al
 * compilar), de modo que hay un solo lugar donde cambiarla; {@code VersionTest} comprueba que el
 * README y el CHANGELOG digan lo mismo.
 */
public final class Version {

    private Version() {
    }

    public static String actual() {
        try (InputStream entrada = Version.class.getResourceAsStream("/version.properties")) {
            if (entrada == null) {
                throw new IllegalStateException("Falta version.properties: compile con Maven");
            }
            Properties propiedades = new Properties();
            propiedades.load(entrada);
            return propiedades.getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer version.properties", e);
        }
    }
}
