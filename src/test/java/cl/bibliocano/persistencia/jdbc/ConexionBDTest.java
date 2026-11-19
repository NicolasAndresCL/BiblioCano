package cl.bibliocano.persistencia.jdbc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.bibliocano.Contexto;
import cl.bibliocano.dominio.Bibliotecario;
import java.time.Clock;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConexionBD: configuración y lectura de scripts")
class ConexionBDTest {

    @Test
    @DisplayName("sin variables de entorno usa los valores del docker-compose")
    void valoresPorDefecto() {
        assertEquals(ConexionBD.URL_POR_DEFECTO, ConexionBD.desde(Map.of()).getUrl());
        assertEquals("jdbc:x", ConexionBD.desde(Map.of(ConexionBD.VARIABLE_URL, "jdbc:x")).getUrl());
    }

    @Test
    @DisplayName("separa sentencias por «;» de fin de línea e ignora comentarios")
    void separaSentencias() {
        String script = """
                -- comentario; con punto y coma
                CREATE TABLE a (x INT);
                INSERT INTO a VALUES (1),
                                     (2);

                SELECT 1;
                """;

        assertArrayEquals(new String[] {"CREATE TABLE a (x INT)",
            "INSERT INTO a VALUES (1),\n                     (2)", "SELECT 1"},
                ConexionBD.separarSentencias(script));
    }

    @Test
    @DisplayName("los dos scripts versionados se pueden separar en sentencias")
    void scriptsReales() throws Exception {
        for (String ruta : new String[] {"/db/01-esquema.sql", "/db/02-datos-demo.sql"}) {
            try (var entrada = ConexionBD.class.getResourceAsStream(ruta)) {
                String texto = new String(entrada.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(ConexionBD.separarSentencias(texto).length > 1, ruta);
            }
        }
    }

    @Test
    @DisplayName("sin base de datos, el error es técnico (PersistenciaException) y conserva la causa")
    void sinBaseDeDatos() {
        ConexionBD inexistente = new ConexionBD("jdbc:postgresql://127.0.0.1:1/nada", "x", "x");
        Contexto contexto = Contexto.postgres(inexistente, Clock.systemDefaultZone());

        PersistenciaException error = assertThrows(PersistenciaException.class,
                () -> contexto.getCatalogo().buscar(""));
        assertTrue(error.getCause() instanceof java.sql.SQLException);
        assertThrows(PersistenciaException.class, () -> contexto.getAcceso().iniciarSesion("a", "b"));
        assertThrows(PersistenciaException.class, inexistente::reiniciarDatosDemo);
        Bibliotecario b = new Bibliotecario(1, "Ana", "ana", "1:AA==:AA==");
        assertThrows(PersistenciaException.class, () -> contexto.getPrestamos().registrarPrestamo(b, 1, "EJ"));
        assertThrows(PersistenciaException.class, () -> contexto.getPrestamos().registrarDevolucion(b, "EJ"));
    }
}
