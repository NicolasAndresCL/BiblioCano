package cl.bibliocano.persistencia.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Datos de conexión a PostgreSQL y utilidades para abrir conexiones y reiniciar los datos de ejemplo.
 *
 * <p>La configuración sale de variables de entorno (nunca del código), con valores por defecto que
 * coinciden con el {@code docker-compose.yml}: así IntelliJ se conecta sin configurar nada.
 */
public class ConexionBD {

    public static final String VARIABLE_URL = "BIBLIOCANO_DB_URL";
    public static final String VARIABLE_USUARIO = "BIBLIOCANO_DB_USUARIO";
    public static final String VARIABLE_PASSWORD = "BIBLIOCANO_DB_PASSWORD";

    static final String URL_POR_DEFECTO = "jdbc:postgresql://localhost:5433/bibliocano";
    static final String USUARIO_POR_DEFECTO = "bibliocano";
    static final String PASSWORD_POR_DEFECTO = "bibliocano";

    /** Scripts que dejan la base como recién creada, en orden. */
    private static final String[] SCRIPTS = {"/db/01-esquema.sql", "/db/02-datos-demo.sql"};

    private final String url;
    private final String usuario;
    private final String password;

    public ConexionBD(String url, String usuario, String password) {
        this.url = Objects.requireNonNull(url, "url");
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.password = Objects.requireNonNull(password, "password");
    }

    public static ConexionBD desdeEntorno() {
        return desde(System.getenv());
    }

    static ConexionBD desde(Map<String, String> entorno) {
        return new ConexionBD(
                entorno.getOrDefault(VARIABLE_URL, URL_POR_DEFECTO),
                entorno.getOrDefault(VARIABLE_USUARIO, USUARIO_POR_DEFECTO),
                entorno.getOrDefault(VARIABLE_PASSWORD, PASSWORD_POR_DEFECTO));
    }

    public Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, usuario, password);
    }

    public String getUrl() {
        return url;
    }

    /**
     * Borra todo y vuelve a cargar el esquema y los datos de ejemplo. Lo usan la demostración por
     * consola (para que cada ejecución muestre lo mismo) y las pruebas de integración.
     */
    public void reiniciarDatosDemo() {
        try (Connection conexion = abrir(); Statement sentencia = conexion.createStatement()) {
            for (String script : SCRIPTS) {
                for (String sql : separarSentencias(leerRecurso(script))) {
                    sentencia.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo reiniciar la base en " + url, e);
        }
    }

    /**
     * Separa un script en sentencias por el «;» de fin de línea. Basta para estos scripts (sin
     * funciones ni «;» dentro de textos); no es un intérprete SQL general.
     */
    static String[] separarSentencias(String script) {
        StringBuilder limpio = new StringBuilder();
        for (String linea : script.split("\\R")) {
            if (!linea.strip().startsWith("--")) {
                limpio.append(linea).append('\n');
            }
        }
        return Arrays.stream(limpio.toString().split(";\\s*\\n"))
                .map(String::strip)
                .map(s -> s.endsWith(";") ? s.substring(0, s.length() - 1) : s)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    private static String leerRecurso(String ruta) {
        try (InputStream entrada = ConexionBD.class.getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new PersistenciaException("No se encontró el recurso " + ruta, null);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer " + ruta, e);
        }
    }
}
