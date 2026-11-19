package cl.bibliocano.dominio;

import static cl.bibliocano.dominio.Fabrica.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Préstamo: plazo, vencimiento y multa")
class PrestamoTest {

    @ParameterizedTest(name = "entregado {0} días después del préstamo → multa ${1}")
    @CsvSource({
        "0,  0",     // el mismo día
        "14, 0",     // el último día del plazo
        "15, 500",   // un día de atraso
        "20, 3000",  // seis días de atraso
    })
    @DisplayName("calcula $500 por cada día de atraso y nada dentro del plazo")
    void calculaMulta(int diasDespues, int multaEsperada) {
        Prestamo prestamo = Fabrica.prestamoDeHace(0, Fabrica.socio());

        assertEquals(multaEsperada, prestamo.calcularMulta(HOY.plusDays(diasDespues)));
    }

    @Test
    @DisplayName("está vencido solo si sigue activo y pasó la fecha límite")
    void vencimiento() {
        Prestamo prestamo = Fabrica.prestamoDeHace(0, Fabrica.socio());
        LocalDate limite = prestamo.getFechaDevolucionEsperada();

        assertFalse(prestamo.estaVencido(limite), "el día límite todavía no está vencido");
        assertTrue(prestamo.estaVencido(limite.plusDays(1)));

        prestamo.finalizarPrestamo(limite.plusDays(1));
        assertFalse(prestamo.estaVencido(limite.plusDays(30)), "un préstamo devuelto nunca está vencido");
    }

    @Test
    @DisplayName("al devolver a tiempo: libera el ejemplar, sin multa y el socio queda habilitado")
    void devolucionATiempo() {
        Socio socio = Fabrica.socio();
        Prestamo prestamo = Fabrica.prestamoDeHace(5, socio);

        prestamo.finalizarPrestamo(HOY);

        assertEquals(EstadoPrestamo.DEVUELTO, prestamo.getEstado());
        assertEquals(HOY, prestamo.getFechaDevolucionReal());
        assertEquals(0, prestamo.getMulta());
        assertEquals(EstadoEjemplar.DISPONIBLE, prestamo.getEjemplar().getEstado());
        assertTrue(socio.getPrestamosActivos().isEmpty());
        assertTrue(socio.validarEstado(HOY));
    }

    @Test
    @DisplayName("al devolver atrasado: cobra la multa y deja al socio con multa impaga")
    void devolucionAtrasada() {
        Socio socio = Fabrica.socio();
        Prestamo prestamo = Fabrica.prestamoDeHace(Prestamo.DIAS_PLAZO + 6, socio);

        prestamo.finalizarPrestamo(HOY);

        assertEquals(3000, prestamo.getMulta());
        assertTrue(socio.isTieneMulta());
        assertFalse(socio.validarEstado(HOY), "con multa impaga no puede pedir otro préstamo");
    }

    @Test
    @DisplayName("no permite devolver dos veces el mismo préstamo")
    void noDevuelveDosVeces() {
        Prestamo prestamo = Fabrica.prestamoDeHace(1, Fabrica.socio());
        prestamo.finalizarPrestamo(HOY);

        assertThrows(IllegalStateException.class, () -> prestamo.finalizarPrestamo(HOY));
    }

    @Test
    @DisplayName("no permite una devolución anterior a la fecha del préstamo")
    void noDevuelveAntesDePrestar() {
        Prestamo prestamo = Fabrica.prestamoDeHace(0, Fabrica.socio());

        assertThrows(IllegalArgumentException.class, () -> prestamo.finalizarPrestamo(HOY.minusDays(1)));
        assertNull(prestamo.getFechaDevolucionReal());
    }

    @Test
    @DisplayName("el id lo asigna la persistencia una sola vez")
    void asignaIdUnaVez() {
        Prestamo prestamo = Fabrica.prestamoDeHace(0, Fabrica.socio());
        prestamo.asignarId(7);

        assertEquals(7, prestamo.getIdPrestamo());
        assertThrows(IllegalStateException.class, () -> prestamo.asignarId(8));
        assertTrue(prestamo.toString().contains("Préstamo 7"));
    }

    @Test
    @DisplayName("reconstituir un préstamo devuelto no lo agrega a los activos del socio")
    void reconstituirDevuelto() {
        Socio socio = Fabrica.socio();
        Prestamo.reconstituir(3, socio, Fabrica.ejemplar(), Fabrica.bibliotecario(), HOY.minusDays(20),
                HOY.minusDays(6), HOY.minusDays(1), EstadoPrestamo.DEVUELTO, 2500);

        assertTrue(socio.getPrestamosActivos().isEmpty());
    }
}
