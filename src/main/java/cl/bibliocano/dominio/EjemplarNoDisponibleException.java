package cl.bibliocano.dominio;

/**
 * El ejemplar está prestado o en mantenimiento.
 *
 * <p>También se lanza cuando otro bibliotecario prestó la misma copia un instante antes (la base
 * de datos rechaza el segundo préstamo; ver {@code JdbcRepositorioPrestamos#registrar}).
 */
public final class EjemplarNoDisponibleException extends BibliotecaException {

    private static final long serialVersionUID = 1L;

    public EjemplarNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public EjemplarNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
