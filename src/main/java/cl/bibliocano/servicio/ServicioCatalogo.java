package cl.bibliocano.servicio;

import cl.bibliocano.dominio.Libro;
import java.util.List;
import java.util.Objects;

/** Caso de uso «Consultar catálogo de libros». */
public class ServicioCatalogo {

    private final RepositorioLibros libros;

    public ServicioCatalogo(RepositorioLibros libros) {
        this.libros = Objects.requireNonNull(libros, "libros");
    }

    /** Busca por título, autor o ISBN. {@code null} o vacío lista todo el catálogo. */
    public List<Libro> buscar(String texto) {
        return libros.buscar(texto == null ? "" : texto.strip());
    }
}
