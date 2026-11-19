package cl.bibliocano.dominio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lector inscrito en la biblioteca.
 *
 * <p>Conoce sus préstamos activos (asociación {@code Socio 1 ── 0..* Prestamo}) porque la regla
 * «no prestar a quien tiene libros vencidos» es una pregunta sobre el propio socio: por eso
 * {@link #validarEstado(LocalDate)} vive aquí y no en un servicio (experto en información).
 */
public class Socio {

    private final int idSocio;
    private final String rut;
    private final String nombre;
    private final String correo;
    private boolean tieneMulta;
    private final List<Prestamo> prestamosActivos = new ArrayList<>();

    public Socio(int idSocio, String rut, String nombre, String correo, boolean tieneMulta) {
        if (idSocio <= 0) {
            throw new IllegalArgumentException("El id del socio debe ser positivo");
        }
        this.idSocio = idSocio;
        this.rut = Libro.textoObligatorio(rut, "RUT");
        this.nombre = Libro.textoObligatorio(nombre, "nombre");
        this.correo = Libro.textoObligatorio(correo, "correo");
        this.tieneMulta = tieneMulta;
    }

    /**
     * Regla de habilitación: puede recibir un préstamo quien no tiene multas impagas ni préstamos
     * vencidos a la fecha {@code hoy}.
     */
    public boolean validarEstado(LocalDate hoy) {
        return !tieneMulta && !tienePrestamosVencidos(hoy);
    }

    public boolean tienePrestamosVencidos(LocalDate hoy) {
        for (Prestamo prestamo : prestamosActivos) {
            if (prestamo.estaVencido(hoy)) {
                return true;
            }
        }
        return false;
    }

    /** Marca la multa impaga que deja una devolución atrasada. */
    public void registrarMulta() {
        this.tieneMulta = true;
    }

    /** Lo llama el constructor de {@link Prestamo}: mantiene los dos lados de la asociación. */
    void agregarPrestamo(Prestamo prestamo) {
        prestamosActivos.add(prestamo);
    }

    /** Lo llama {@link Prestamo#finalizarPrestamo(LocalDate)} al cerrar el préstamo. */
    void quitarPrestamo(Prestamo prestamo) {
        prestamosActivos.remove(prestamo);
    }

    public List<Prestamo> getPrestamosActivos() {
        return Collections.unmodifiableList(prestamosActivos);
    }

    public int getIdSocio() {
        return idSocio;
    }

    public String getRut() {
        return rut;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public boolean isTieneMulta() {
        return tieneMulta;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Socio socio && idSocio == socio.idSocio;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idSocio);
    }

    @Override
    public String toString() {
        return nombre + " (socio " + idSocio + ")";
    }
}
