package cl.bibliocano.dominio;

/**
 * Usuario o contraseña incorrectos.
 *
 * <p>El mensaje es el mismo en ambos casos a propósito: decir «el usuario no existe» le confirma a
 * un atacante qué nombres de usuario son válidos.
 */
public final class CredencialesInvalidasException extends BibliotecaException {

    private static final long serialVersionUID = 1L;

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }

    public CredencialesInvalidasException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
