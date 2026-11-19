package cl.bibliocano.dominio;

import java.util.Objects;

/**
 * Copia física de un {@link Libro}, identificada por su código de barras.
 *
 * <p>Es lo que se presta: el socio se lleva un ejemplar concreto, no «el libro». Su constructor es
 * de paquete porque solo {@link Libro#agregarEjemplar(String, String)} puede crearlo (composición).
 */
public class Ejemplar {

    private final String codigoBarra;
    private final String ubicacion;
    private final Libro libro;
    private EstadoEjemplar estado;

    Ejemplar(String codigoBarra, String ubicacion, Libro libro) {
        this.codigoBarra = Libro.textoObligatorio(codigoBarra, "código de barras");
        this.ubicacion = Libro.textoObligatorio(ubicacion, "ubicación");
        this.libro = Objects.requireNonNull(libro, "libro");
        this.estado = EstadoEjemplar.DISPONIBLE;
    }

    public boolean estaDisponible() {
        return estado == EstadoEjemplar.DISPONIBLE;
    }

    public void cambiarEstado(EstadoEjemplar nuevoEstado) {
        this.estado = Objects.requireNonNull(nuevoEstado, "nuevoEstado");
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public EstadoEjemplar getEstado() {
        return estado;
    }

    public Libro getLibro() {
        return libro;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Ejemplar ejemplar && codigoBarra.equals(ejemplar.codigoBarra);
    }

    @Override
    public int hashCode() {
        return codigoBarra.hashCode();
    }

    @Override
    public String toString() {
        return codigoBarra + " · " + libro.getTitulo() + " · " + estado;
    }
}
