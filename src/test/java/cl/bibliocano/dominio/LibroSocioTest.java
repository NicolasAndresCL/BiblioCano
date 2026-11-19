package cl.bibliocano.dominio;

import static cl.bibliocano.dominio.Fabrica.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Libro, Ejemplar, Socio y Contrasenas")
class LibroSocioTest {

    @Test
    @DisplayName("Libro crea sus ejemplares disponibles y ligados a él (composición)")
    void libroCreaEjemplares() {
        Libro libro = Fabrica.libro();

        Ejemplar ejemplar = libro.agregarEjemplar("EJ-0001", "Estante A-1");

        assertSame(libro, ejemplar.getLibro());
        assertTrue(ejemplar.estaDisponible());
        assertEquals(List.of(ejemplar), libro.getEjemplares());
        assertThrows(UnsupportedOperationException.class, () -> libro.getEjemplares().clear(),
                "las copias solo se agregan con agregarEjemplar");
    }

    @Test
    @DisplayName("Libro rechaza un código de barras repetido")
    void rechazaCodigoRepetido() {
        Libro libro = Fabrica.libro();
        libro.agregarEjemplar("EJ-0001", "Estante A-1");

        assertThrows(IllegalArgumentException.class, () -> libro.agregarEjemplar("EJ-0001", "Bodega"));
    }

    @Test
    @DisplayName("obtenerEjemplaresDisponibles excluye los prestados y en mantenimiento")
    void filtraDisponibles() {
        Libro libro = Fabrica.libro();
        Ejemplar disponible = libro.agregarEjemplar("EJ-1", "A");
        libro.agregarEjemplar("EJ-2", "A").cambiarEstado(EstadoEjemplar.PRESTADO);
        libro.agregarEjemplar("EJ-3", "A").cambiarEstado(EstadoEjemplar.MANTENIMIENTO);

        assertEquals(List.of(disponible), libro.obtenerEjemplaresDisponibles());
    }

    @ParameterizedTest(name = "rechaza el título «{0}»")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Libro exige datos no vacíos")
    void exigeDatos(String titulo) {
        assertThrows(IllegalArgumentException.class, () -> new Libro("123", titulo, "Autor", "Cat"));
        assertThrows(NullPointerException.class, () -> new Libro(null, "T", "A", "C"));
    }

    @Test
    @DisplayName("igualdad por clave: ISBN, código de barras e id de socio")
    void igualdadPorClave() {
        assertEquals(Fabrica.libro(), Fabrica.libro());
        assertEquals(Fabrica.libro().hashCode(), Fabrica.libro().hashCode());
        assertEquals(Fabrica.ejemplar(), Fabrica.ejemplar());
        assertEquals(Fabrica.socio(), Fabrica.socio());
        assertNotEquals(Fabrica.socio(), new Socio(2, "1-9", "Otro", "o@o.cl", false));
        assertTrue(Fabrica.ejemplar().toString().contains("EJ-0001"));
        assertTrue(Fabrica.libro().toString().contains("Clean Code"));
    }

    @Test
    @DisplayName("Socio habilitado: sin multa y sin vencidos")
    void socioHabilitado() {
        Socio socio = Fabrica.socio();
        assertTrue(socio.validarEstado(HOY));

        socio.registrarMulta();
        assertFalse(socio.validarEstado(HOY));
        assertTrue(socio.toString().contains("Camila"));
    }

    @Test
    @DisplayName("Socio exige id positivo")
    void socioIdPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new Socio(0, "1-9", "A", "a@a.cl", false));
    }

    @Test
    @DisplayName("Contrasenas: la misma clave da hashes distintos (sal) y ambos la validan")
    void hashConSal() {
        String h1 = Contrasenas.generarHash("biblio2026");
        String h2 = Contrasenas.generarHash("biblio2026");

        assertNotEquals(h1, h2);
        assertTrue(Contrasenas.coincide("biblio2026", h1));
        assertTrue(Contrasenas.coincide("biblio2026", h2));
        assertFalse(Contrasenas.coincide("biblio2025", h1));
    }

    @ParameterizedTest(name = "un hash mal formado («{0}») nunca coincide")
    @ValueSource(strings = {"", "sin-separadores", "1:2", "abc:AAAA:BBBB", "1000:%%%:BBBB"})
    void hashMalFormado(String guardado) {
        assertFalse(Contrasenas.coincide("x", guardado));
    }
}
