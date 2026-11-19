package cl.bibliocano.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Título del catálogo (la obra), identificado por su ISBN.
 *
 * <p>Es el «todo» de la composición {@code Libro 1 ◆── 0..* Ejemplar}: la única forma de crear un
 * {@link Ejemplar} es {@link #agregarEjemplar(String, String)}, de modo que no existe una copia
 * física sin su título. La multiplicidad es {@code 0..*} porque un título puede registrarse antes de
 * que lleguen sus copias, o quedarse sin ellas si se dan de baja.
 */
public class Libro {

    private final String isbn;
    private final String titulo;
    private final String autor;
    private final String categoria;
    private final List<Ejemplar> ejemplares = new ArrayList<>();

    public Libro(String isbn, String titulo, String autor, String categoria) {
        this.isbn = textoObligatorio(isbn, "ISBN");
        this.titulo = textoObligatorio(titulo, "título");
        this.autor = textoObligatorio(autor, "autor");
        this.categoria = textoObligatorio(categoria, "categoría");
    }

    /**
     * Crea una copia física de este título y la deja {@link EstadoEjemplar#DISPONIBLE}.
     *
     * @throws IllegalArgumentException si el código de barras ya pertenece a otra copia del título
     */
    public Ejemplar agregarEjemplar(String codigoBarra, String ubicacion) {
        for (Ejemplar existente : ejemplares) {
            if (existente.getCodigoBarra().equals(codigoBarra)) {
                throw new IllegalArgumentException("El código " + codigoBarra + " ya está registrado");
            }
        }
        Ejemplar ejemplar = new Ejemplar(codigoBarra, ubicacion, this);
        ejemplares.add(ejemplar);
        return ejemplar;
    }

    /** Copias que hoy se pueden prestar. */
    public List<Ejemplar> obtenerEjemplaresDisponibles() {
        List<Ejemplar> disponibles = new ArrayList<>();
        for (Ejemplar ejemplar : ejemplares) {
            if (ejemplar.estaDisponible()) {
                disponibles.add(ejemplar);
            }
        }
        return disponibles;
    }

    /** Vista de solo lectura: las copias se agregan únicamente con {@link #agregarEjemplar}. */
    public List<Ejemplar> getEjemplares() {
        return Collections.unmodifiableList(ejemplares);
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutor() {
        return autor;
    }

    public String getCategoria() {
        return categoria;
    }

    /** Dos libros son el mismo si tienen el mismo ISBN (su clave en el modelo de datos). */
    @Override
    public boolean equals(Object otro) {
        return otro instanceof Libro libro && isbn.equals(libro.isbn);
    }

    @Override
    public int hashCode() {
        return isbn.hashCode();
    }

    @Override
    public String toString() {
        return titulo + " (" + autor + ", ISBN " + isbn + ")";
    }

    static String textoObligatorio(String valor, String campo) {
        Objects.requireNonNull(valor, campo);
        if (valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " no puede estar vacío");
        }
        return valor.strip();
    }
}
