package cl.bibliocano;

import cl.bibliocano.consola.DemoConsola;
import cl.bibliocano.persistencia.jdbc.ConexionBD;
import cl.bibliocano.ui.VentanaLogin;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;

/**
 * Punto de entrada.
 *
 * <pre>
 *   java -jar bibliocano.jar [gui | demo | reiniciar-datos] [--memoria]
 * </pre>
 *
 * <ul>
 *   <li>{@code gui} (por defecto): interfaz Swing. Necesita pantalla: se corre desde IntelliJ.</li>
 *   <li>{@code demo}: recorre por consola los escenarios de prueba (éxito y errores).</li>
 *   <li>{@code reiniciar-datos}: deja PostgreSQL con los datos de ejemplo originales.</li>
 *   <li>{@code --memoria}: usa datos en memoria en vez de PostgreSQL (sin Docker).</li>
 * </ul>
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        List<String> argumentos = Arrays.asList(args);
        boolean enMemoria = argumentos.contains("--memoria")
                || "memoria".equalsIgnoreCase(System.getenv("BIBLIOCANO_MODO"));
        String comando = argumentos.stream().filter(a -> !a.startsWith("--")).findFirst().orElse("gui");
        Clock reloj = Clock.systemDefaultZone();

        switch (comando) {
            case "demo" -> {
                Contexto contexto;
                if (enMemoria) {
                    contexto = Contexto.memoria(reloj);
                } else {
                    ConexionBD conexion = ConexionBD.desdeEntorno();
                    conexion.reiniciarDatosDemo(); // cada demo parte de los mismos datos
                    contexto = Contexto.postgres(conexion, reloj);
                }
                int fallos = new DemoConsola(contexto, System.out).ejecutar();
                System.exit(fallos == 0 ? 0 : 1);
            }
            case "reiniciar-datos" -> {
                ConexionBD conexion = ConexionBD.desdeEntorno();
                conexion.reiniciarDatosDemo();
                System.out.println("Datos de ejemplo cargados en " + conexion.getUrl());
            }
            case "gui" -> {
                Contexto contexto = enMemoria ? Contexto.memoria(reloj)
                        : Contexto.postgres(ConexionBD.desdeEntorno(), reloj);
                VentanaLogin.mostrar(contexto);
            }
            default -> {
                System.err.println("Comando desconocido: " + comando
                        + ". Use: gui | demo | reiniciar-datos [--memoria]");
                System.exit(2);
            }
        }
    }
}
