package cl.bibliocano.ui;

import cl.bibliocano.dominio.BibliotecaException;
import cl.bibliocano.persistencia.jdbc.PersistenciaException;
import java.awt.Component;
import java.util.function.Supplier;
import javax.swing.JOptionPane;

/**
 * Único punto donde la interfaz traduce excepciones a mensajes. Distingue una regla de negocio
 * incumplida (aviso amarillo, el bibliotecario puede corregir) de una falla técnica (error rojo).
 */
final class Dialogos {

    private Dialogos() {
    }

    /** Ejecuta la acción; si falla, muestra el mensaje y devuelve {@code null}. */
    static <T> T ejecutar(Component padre, Supplier<T> accion) {
        try {
            return accion.get();
        } catch (BibliotecaException e) {
            JOptionPane.showMessageDialog(padre, e.getMessage(), "Operación rechazada",
                    JOptionPane.WARNING_MESSAGE);
        } catch (PersistenciaException e) {
            JOptionPane.showMessageDialog(padre,
                    "No hay conexión con la base de datos.\n¿Está arriba «docker compose up -d db»?\n\n"
                            + e.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(padre, e.getMessage(), "Dato inválido", JOptionPane.WARNING_MESSAGE);
        }
        return null;
    }
}
