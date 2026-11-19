package cl.bibliocano.dominio;

import java.time.LocalDate;

/** Objetos de prueba con valores válidos, para que cada test cambie solo lo que le importa. */
final class Fabrica {

    static final LocalDate HOY = LocalDate.of(2026, 11, 26);
    /** Hash precalculado una vez: PBKDF2 es lento a propósito y no conviene repetirlo en cada test. */
    static final String HASH_CLAVE = Contrasenas.generarHash("clave-correcta");

    private Fabrica() {
    }

    static Libro libro() {
        return new Libro("9780132350884", "Clean Code", "Robert C. Martin", "Ingeniería de software");
    }

    static Ejemplar ejemplar() {
        return libro().agregarEjemplar("EJ-0001", "Estante A-1");
    }

    static Socio socio() {
        return new Socio(1, "12.345.678-5", "Camila Fuentes", "camila@correo.cl", false);
    }

    static Bibliotecario bibliotecario() {
        return new Bibliotecario(1, "Ana Rojas", "bibliotecaria", HASH_CLAVE);
    }

    /** Préstamo activo creado {@code diasAtras} días antes de {@link #HOY}. */
    static Prestamo prestamoDeHace(int diasAtras, Socio socio) {
        Ejemplar ejemplar = libro().agregarEjemplar("EJ-0" + diasAtras, "Estante");
        return bibliotecario().registrarPrestamo(socio, ejemplar, HOY.minusDays(diasAtras));
    }
}
