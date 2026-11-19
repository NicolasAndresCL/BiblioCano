package cl.bibliocano.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.bibliocano.dominio.Bibliotecario;
import cl.bibliocano.dominio.EjemplarNoDisponibleException;
import cl.bibliocano.dominio.EstadoEjemplar;
import cl.bibliocano.dominio.EstadoPrestamo;
import cl.bibliocano.dominio.Prestamo;
import cl.bibliocano.dominio.RecursoNoEncontradoException;
import cl.bibliocano.dominio.SocioInhabilitadoException;
import cl.bibliocano.persistencia.memoria.BibliotecaEnMemoria;
import cl.bibliocano.persistencia.memoria.DatosDemo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Casos de uso «Registrar préstamo» y «Registrar devolución» de punta a punta, con los mismos datos
 * de la demostración y un reloj fijo (26/11/2026, fecha de ejemplo).
 */
@DisplayName("ServicioPrestamos con los datos de ejemplo")
class ServicioPrestamosTest {

    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-26T15:00:00Z"), SANTIAGO);
    private static final LocalDate HOY = LocalDate.now(RELOJ);

    private BibliotecaEnMemoria memoria;
    private ServicioPrestamos servicio;
    private Bibliotecario bibliotecaria;

    @BeforeEach
    void preparar() {
        memoria = DatosDemo.crear(HOY);
        servicio = new ServicioPrestamos(memoria, memoria, memoria, RELOJ);
        bibliotecaria = new ServicioAcceso(memoria).iniciarSesion(DatosDemo.USUARIO, DatosDemo.PASSWORD);
    }

    @Test
    @DisplayName("ÉXITO: registra el préstamo, le asigna id y lo deja guardado")
    void prestamoExitoso() {
        Prestamo prestamo = servicio.registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_HABILITADO,
                DatosDemo.EJEMPLAR_DISPONIBLE);

        assertEquals(2, prestamo.getIdPrestamo(), "el préstamo sembrado es el 1");
        assertEquals(HOY.plusDays(14), prestamo.getFechaDevolucionEsperada());
        assertEquals(EstadoEjemplar.PRESTADO, prestamo.getEjemplar().getEstado());
        assertEquals(prestamo, memoria.buscarActivoPorEjemplar(DatosDemo.EJEMPLAR_DISPONIBLE).orElseThrow());
    }

    @Test
    @DisplayName("ERROR: el mismo ejemplar no se puede prestar dos veces")
    void noPrestaDosVeces() {
        servicio.registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_HABILITADO, DatosDemo.EJEMPLAR_DISPONIBLE);

        assertThrows(EjemplarNoDisponibleException.class, () -> servicio.registrarPrestamo(bibliotecaria,
                DatosDemo.SOCIO_HABILITADO, DatosDemo.EJEMPLAR_DISPONIBLE));
        assertEquals(2, memoria.listarPrestamos().size(), "no se guardó un segundo préstamo");
    }

    @Test
    @DisplayName("ERROR: socio con multa y socio con préstamo vencido")
    void sociosInhabilitados() {
        assertThrows(SocioInhabilitadoException.class,
                () -> servicio.registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_CON_MULTA, "EJ-0003"));
        assertThrows(SocioInhabilitadoException.class,
                () -> servicio.registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_CON_VENCIDO, "EJ-0003"));
        assertEquals(1, memoria.listarPrestamos().size());
    }

    @Test
    @DisplayName("ERROR: ejemplar en mantenimiento")
    void ejemplarEnMantenimiento() {
        assertThrows(EjemplarNoDisponibleException.class, () -> servicio.registrarPrestamo(bibliotecaria,
                DatosDemo.SOCIO_HABILITADO, DatosDemo.EJEMPLAR_EN_MANTENIMIENTO));
    }

    @Test
    @DisplayName("ERROR: socio o ejemplar inexistente")
    void recursosInexistentes() {
        RecursoNoEncontradoException sinSocio = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.registrarPrestamo(bibliotecaria, 99, "EJ-0003"));
        RecursoNoEncontradoException sinEjemplar = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_HABILITADO, "EJ-9999"));

        assertTrue(sinSocio.getMessage().contains("99"));
        assertTrue(sinEjemplar.getMessage().contains("EJ-9999"));
    }

    @Test
    @DisplayName("DEVOLUCIÓN atrasada: multa de $3.000 y el socio queda bloqueado para nuevos préstamos")
    void devolucionAtrasadaBloqueaAlSocio() {
        Prestamo devuelto = servicio.registrarDevolucion(bibliotecaria, DatosDemo.EJEMPLAR_PRESTADO);

        assertEquals(EstadoPrestamo.DEVUELTO, devuelto.getEstado());
        assertEquals(3000, devuelto.getMulta());
        assertEquals(EstadoEjemplar.DISPONIBLE, devuelto.getEjemplar().getEstado());
        // Ya no tiene vencidos, pero ahora tiene multa: sigue sin poder pedir libros
        assertThrows(SocioInhabilitadoException.class,
                () -> servicio.registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_CON_VENCIDO, "EJ-0003"));
    }

    @Test
    @DisplayName("DEVOLUCIÓN a tiempo: sin multa y el ejemplar se puede volver a prestar")
    void devolucionATiempo() {
        servicio.registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_HABILITADO, DatosDemo.EJEMPLAR_DISPONIBLE);

        Prestamo devuelto = servicio.registrarDevolucion(bibliotecaria, DatosDemo.EJEMPLAR_DISPONIBLE);

        assertEquals(0, devuelto.getMulta());
        Prestamo otraVez = servicio.registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_HABILITADO,
                DatosDemo.EJEMPLAR_DISPONIBLE);
        assertEquals(EstadoPrestamo.ACTIVO, otraVez.getEstado());
    }

    @Test
    @DisplayName("ERROR: devolver un ejemplar que no está prestado")
    void devolucionSinPrestamo() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.registrarDevolucion(bibliotecaria, "EJ-0003"));
    }

    @Test
    @DisplayName("el repositorio rechaza registrar un préstamo que no conoce como devuelto")
    void devolucionDePrestamoNoRegistrado() {
        Prestamo ajeno = DatosDemo.crear(HOY).buscarActivoPorEjemplar(DatosDemo.EJEMPLAR_PRESTADO).orElseThrow();

        assertThrows(IllegalArgumentException.class, () -> memoria.registrarDevolucion(ajeno));
    }
}
