package cl.bibliocano.dominio;

/** Se pidió un socio, ejemplar o préstamo activo que no existe. */
public final class RecursoNoEncontradoException extends BibliotecaException {

    private static final long serialVersionUID = 1L;

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public RecursoNoEncontradoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
