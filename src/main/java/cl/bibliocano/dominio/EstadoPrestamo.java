package cl.bibliocano.dominio;

/**
 * Estados que se almacenan de un préstamo.
 *
 * <p>«Vencido» no es un estado guardado sino una condición derivada: un préstamo {@link #ACTIVO}
 * cuya fecha esperada ya pasó (ver {@link Prestamo#estaVencido(java.time.LocalDate)}). Guardarlo
 * exigiría un proceso que lo actualice cada día y, si ese proceso falla, la base mentiría.
 */
public enum EstadoPrestamo {
    ACTIVO,
    DEVUELTO
}
