package cl.bibliocano.dominio;

/**
 * Estados posibles de una copia física.
 *
 * <p>Solo un ejemplar {@link #DISPONIBLE} puede prestarse. {@link #MANTENIMIENTO} cubre la copia
 * retirada de circulación (reparación, extravío en revisión): existe en el catálogo pero no se presta.
 */
public enum EstadoEjemplar {
    DISPONIBLE,
    PRESTADO,
    MANTENIMIENTO
}
