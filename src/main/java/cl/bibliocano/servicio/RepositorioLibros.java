package cl.bibliocano.servicio;

import cl.bibliocano.dominio.Libro;
import java.util.List;

/** Consulta del catálogo. */
public interface RepositorioLibros {

    /**
     * Libros cuyo título, autor o ISBN contienen {@code texto} (sin distinguir mayúsculas), con sus
     * ejemplares cargados. Un texto vacío devuelve el catálogo completo.
     */
    List<Libro> buscar(String texto);
}
