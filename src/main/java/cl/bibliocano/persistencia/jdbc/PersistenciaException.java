package cl.bibliocano.persistencia.jdbc;

/**
 * Falla técnica de la base de datos (caída, credenciales, SQL inválido).
 *
 * <p>No hereda de {@code BibliotecaException} a propósito: no es una regla de negocio incumplida
 * sino un problema de infraestructura, y la interfaz lo informa distinto («no hay conexión»).
 * Conserva la {@link java.sql.SQLException} original como causa.
 */
public class PersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
