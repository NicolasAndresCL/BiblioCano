package cl.bibliocano.servicio;

import cl.bibliocano.dominio.EjemplarNoDisponibleException;
import cl.bibliocano.dominio.Prestamo;
import java.util.Optional;

/** Guarda préstamos y devoluciones. Cada operación es atómica: todo o nada. */
public interface RepositorioPrestamos {

    /**
     * Guarda el préstamo nuevo, marca el ejemplar como prestado y le asigna el id generado.
     *
     * @throws EjemplarNoDisponibleException si otra operación prestó el mismo ejemplar entre la
     *                                       validación y el guardado (préstamo concurrente)
     */
    void registrar(Prestamo prestamo);

    /** El préstamo activo del ejemplar, con su socio, ejemplar y bibliotecario. */
    Optional<Prestamo> buscarActivoPorEjemplar(String codigoBarra);

    /** Guarda el cierre del préstamo, libera el ejemplar y, si hubo multa, la anota en el socio. */
    void registrarDevolucion(Prestamo prestamo);
}
