package cl.bibliocano;

import cl.bibliocano.persistencia.jdbc.BibliotecaPostgres;
import cl.bibliocano.persistencia.jdbc.ConexionBD;
import cl.bibliocano.persistencia.memoria.BibliotecaEnMemoria;
import cl.bibliocano.persistencia.memoria.DatosDemo;
import cl.bibliocano.servicio.ServicioAcceso;
import cl.bibliocano.servicio.ServicioCatalogo;
import cl.bibliocano.servicio.ServicioPrestamos;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Raíz de composición: el único lugar que decide qué implementación de los repositorios se usa.
 *
 * <p>Los servicios reciben sus repositorios por constructor (inyección de dependencias) y no saben
 * si detrás hay PostgreSQL o memoria. Por eso este cambio no toca ni el dominio ni la interfaz.
 */
public final class Contexto {

    private final ServicioAcceso acceso;
    private final ServicioCatalogo catalogo;
    private final ServicioPrestamos prestamos;
    private final String descripcion;

    private Contexto(ServicioAcceso acceso, ServicioCatalogo catalogo, ServicioPrestamos prestamos,
            String descripcion) {
        this.acceso = acceso;
        this.catalogo = catalogo;
        this.prestamos = prestamos;
        this.descripcion = descripcion;
    }

    /** Datos persistentes en PostgreSQL. */
    public static Contexto postgres(ConexionBD conexion, Clock reloj) {
        BibliotecaPostgres bd = new BibliotecaPostgres(conexion);
        return new Contexto(new ServicioAcceso(bd), new ServicioCatalogo(bd),
                new ServicioPrestamos(bd, bd, bd, reloj), "PostgreSQL (" + conexion.getUrl() + ")");
    }

    /** Datos de ejemplo en memoria: se pierden al cerrar. Respaldo si Docker no está disponible. */
    public static Contexto memoria(Clock reloj) {
        BibliotecaEnMemoria memoria = DatosDemo.crear(LocalDate.now(reloj));
        return new Contexto(new ServicioAcceso(memoria), new ServicioCatalogo(memoria),
                new ServicioPrestamos(memoria, memoria, memoria, reloj), "memoria (datos de ejemplo)");
    }

    public ServicioAcceso getAcceso() {
        return acceso;
    }

    public ServicioCatalogo getCatalogo() {
        return catalogo;
    }

    public ServicioPrestamos getPrestamos() {
        return prestamos;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
