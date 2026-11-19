package cl.bibliocano.servicio;

import cl.bibliocano.dominio.Bibliotecario;
import cl.bibliocano.dominio.Ejemplar;
import cl.bibliocano.dominio.Prestamo;
import cl.bibliocano.dominio.RecursoNoEncontradoException;
import cl.bibliocano.dominio.Socio;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Coordina los casos de uso «Registrar préstamo de libro» y «Registrar devolución».
 *
 * <p>No contiene reglas de negocio: busca los objetos, le pide al {@link Bibliotecario} que aplique
 * las reglas y guarda el resultado (patrón Controlador de GRASP). Recibe un {@link Clock} en vez de
 * leer la fecha del sistema para que las pruebas puedan fijar «hoy» y verificar plazos y multas.
 */
public class ServicioPrestamos {

    private final RepositorioSocios socios;
    private final RepositorioEjemplares ejemplares;
    private final RepositorioPrestamos prestamos;
    private final Clock reloj;

    public ServicioPrestamos(RepositorioSocios socios, RepositorioEjemplares ejemplares,
            RepositorioPrestamos prestamos, Clock reloj) {
        this.socios = Objects.requireNonNull(socios, "socios");
        this.ejemplares = Objects.requireNonNull(ejemplares, "ejemplares");
        this.prestamos = Objects.requireNonNull(prestamos, "prestamos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * Funcionalidad representativa del sistema. Los pasos son los mensajes 1.1.1 a 1.1.4 de los
     * diagramas de secuencia y comunicación.
     */
    public Prestamo registrarPrestamo(Bibliotecario bibliotecario, int idSocio, String codigoBarra) {
        Objects.requireNonNull(bibliotecario, "bibliotecario");
        LocalDate hoy = LocalDate.now(reloj);
        // 1.1.1 y 1.1.2: cargar los participantes; si no existen, no hay nada que validar
        Socio socio = socios.buscarPorId(idSocio)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio " + idSocio));
        Ejemplar ejemplar = ejemplares.buscarPorCodigo(codigoBarra)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el ejemplar " + codigoBarra));
        // 1.1.3: el dominio valida las reglas y crea el préstamo
        Prestamo prestamo = bibliotecario.registrarPrestamo(socio, ejemplar, hoy);
        // 1.1.4: guardar de forma atómica (préstamo + estado del ejemplar)
        prestamos.registrar(prestamo);
        return prestamo;
    }

    /** Cierra el préstamo activo del ejemplar y calcula la multa si llegó atrasado. */
    public Prestamo registrarDevolucion(Bibliotecario bibliotecario, String codigoBarra) {
        Objects.requireNonNull(bibliotecario, "bibliotecario");
        Prestamo prestamo = prestamos.buscarActivoPorEjemplar(codigoBarra)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El ejemplar " + codigoBarra + " no tiene un préstamo activo"));
        bibliotecario.registrarDevolucion(prestamo, LocalDate.now(reloj));
        prestamos.registrarDevolucion(prestamo);
        return prestamo;
    }
}
