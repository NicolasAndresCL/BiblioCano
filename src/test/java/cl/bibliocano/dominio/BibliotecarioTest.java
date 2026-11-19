package cl.bibliocano.dominio;

import static cl.bibliocano.dominio.Fabrica.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Bibliotecario: registrar préstamo (funcionalidad central)")
class BibliotecarioTest {

    private final Bibliotecario bibliotecario = Fabrica.bibliotecario();

    @Nested
    @DisplayName("Flujo principal")
    class FlujoPrincipal {

        @Test
        @DisplayName("presta un ejemplar disponible a un socio habilitado")
        void prestaEjemplarDisponible() {
            Socio socio = Fabrica.socio();
            Ejemplar ejemplar = Fabrica.ejemplar();

            Prestamo prestamo = bibliotecario.registrarPrestamo(socio, ejemplar, HOY);

            assertEquals(EstadoPrestamo.ACTIVO, prestamo.getEstado());
            assertEquals(HOY, prestamo.getFechaPrestamo());
            assertEquals(HOY.plusDays(Prestamo.DIAS_PLAZO), prestamo.getFechaDevolucionEsperada());
            assertSame(bibliotecario, prestamo.getBibliotecario());
            assertEquals(EstadoEjemplar.PRESTADO, ejemplar.getEstado(), "el ejemplar deja de estar disponible");
            assertTrue(socio.getPrestamosActivos().contains(prestamo), "el socio conoce su préstamo");
        }

        @Test
        @DisplayName("un socio con un préstamo activo NO vencido puede pedir otro")
        void prestamoActivoNoVencidoNoBloquea() {
            Socio socio = Fabrica.socio();
            Fabrica.prestamoDeHace(3, socio);

            Prestamo segundo = bibliotecario.registrarPrestamo(socio, Fabrica.ejemplar(), HOY);

            assertEquals(2, socio.getPrestamosActivos().size());
            assertEquals(EstadoPrestamo.ACTIVO, segundo.getEstado());
        }
    }

    @Nested
    @DisplayName("Flujos alternativos: el préstamo se rechaza sin modificar nada")
    class FlujosAlternativos {

        @Test
        @DisplayName("rechaza a un socio con multa impaga")
        void rechazaSocioConMulta() {
            Socio socio = new Socio(2, "9.876.543-3", "Diego Morales", "diego@correo.cl", true);
            Ejemplar ejemplar = Fabrica.ejemplar();

            SocioInhabilitadoException error = assertThrows(SocioInhabilitadoException.class,
                    () -> bibliotecario.registrarPrestamo(socio, ejemplar, HOY));

            assertTrue(error.getMessage().contains("multa impaga"), error.getMessage());
            assertTrue(ejemplar.estaDisponible(), "el ejemplar no debe quedar marcado como prestado");
            assertTrue(socio.getPrestamosActivos().isEmpty());
        }

        @Test
        @DisplayName("rechaza a un socio con un préstamo vencido")
        void rechazaSocioConVencido() {
            Socio socio = Fabrica.socio();
            Fabrica.prestamoDeHace(Prestamo.DIAS_PLAZO + 1, socio);
            Ejemplar ejemplar = Fabrica.ejemplar();

            SocioInhabilitadoException error = assertThrows(SocioInhabilitadoException.class,
                    () -> bibliotecario.registrarPrestamo(socio, ejemplar, HOY));

            assertTrue(error.getMessage().contains("préstamo vencido"), error.getMessage());
            assertTrue(ejemplar.estaDisponible());
        }

        @ParameterizedTest(name = "rechaza un ejemplar en estado {0}")
        @EnumSource(value = EstadoEjemplar.class, names = {"PRESTADO", "MANTENIMIENTO"})
        void rechazaEjemplarNoDisponible(EstadoEjemplar estado) {
            Socio socio = Fabrica.socio();
            Ejemplar ejemplar = Fabrica.ejemplar();
            ejemplar.cambiarEstado(estado);

            EjemplarNoDisponibleException error = assertThrows(EjemplarNoDisponibleException.class,
                    () -> bibliotecario.registrarPrestamo(socio, ejemplar, HOY));

            assertTrue(error.getMessage().contains(estado.name()), error.getMessage());
            assertEquals(estado, ejemplar.getEstado(), "el estado del ejemplar no cambia");
            assertTrue(socio.getPrestamosActivos().isEmpty(), "no se crea préstamo");
        }

        @Test
        @DisplayName("valida primero al socio: con multa Y ejemplar prestado, informa la multa")
        void validaSocioAntesQueEjemplar() {
            Socio socio = new Socio(2, "9.876.543-3", "Diego Morales", "diego@correo.cl", true);
            Ejemplar ejemplar = Fabrica.ejemplar();
            ejemplar.cambiarEstado(EstadoEjemplar.PRESTADO);

            assertThrows(SocioInhabilitadoException.class,
                    () -> bibliotecario.registrarPrestamo(socio, ejemplar, HOY));
        }
    }

    @Nested
    @DisplayName("Devolución e inicio de sesión")
    class Otros {

        @Test
        @DisplayName("registrarDevolucion devuelve la multa cobrada")
        void devolucionDevuelveMulta() {
            Socio socio = Fabrica.socio();
            Prestamo prestamo = Fabrica.prestamoDeHace(Prestamo.DIAS_PLAZO + 2, socio);

            assertEquals(2 * Prestamo.MULTA_POR_DIA, bibliotecario.registrarDevolucion(prestamo, HOY));
        }

        @Test
        @DisplayName("autentica solo con la contraseña correcta")
        void autentica() {
            assertTrue(bibliotecario.autenticar("clave-correcta"));
            assertFalse(bibliotecario.autenticar("otra"));
            assertFalse(bibliotecario.autenticar(null));
        }

        @Test
        @DisplayName("toString no expone el hash de la contraseña")
        void toStringSinHash() {
            assertFalse(bibliotecario.toString().contains(Fabrica.HASH_CLAVE));
        }

        @Test
        @DisplayName("exige un id positivo")
        void idPositivo() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Bibliotecario(0, "Ana", "ana", Fabrica.HASH_CLAVE));
        }
    }
}
