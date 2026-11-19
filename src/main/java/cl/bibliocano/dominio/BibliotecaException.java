package cl.bibliocano.dominio;

/**
 * Raíz de los errores de negocio de BiblioCano.
 *
 * <p>Es no comprobada ({@link RuntimeException}) porque representa una regla de negocio que se
 * incumplió, no una falla técnica que quien llama pueda reintentar. La interfaz (consola o Swing)
 * la atrapa en un solo lugar y muestra {@link #getMessage()} al bibliotecario.
 */
public abstract class BibliotecaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected BibliotecaException(String mensaje) {
        super(mensaje);
    }

    protected BibliotecaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
