package cl.bibliocano.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.bibliocano.Contexto;
import cl.bibliocano.dominio.Bibliotecario;
import cl.bibliocano.dominio.EjemplarNoDisponibleException;
import cl.bibliocano.dominio.EstadoEjemplar;
import cl.bibliocano.dominio.Libro;
import cl.bibliocano.dominio.Prestamo;
import cl.bibliocano.dominio.RecursoNoEncontradoException;
import cl.bibliocano.dominio.Socio;
import cl.bibliocano.dominio.SocioInhabilitadoException;
import cl.bibliocano.persistencia.jdbc.BibliotecaPostgres;
import cl.bibliocano.persistencia.jdbc.ConexionBD;
import cl.bibliocano.persistencia.memoria.DatosDemo;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Las mismas reglas, ahora contra PostgreSQL real: comprueba que lo guardado se vuelve a leer igual
 * y que la base impide dos préstamos activos del mismo ejemplar aunque lleguen a la vez.
 *
 * <p>Corre solo si existe {@code BIBLIOCANO_DB_URL} (el CI y {@code scripts/verificar.ps1} la
 * definen). El CI además comprueba que esta clase NO quedó omitida.
 */
@EnabledIfEnvironmentVariable(named = ConexionBD.VARIABLE_URL, matches = ".+")
@DisplayName("Integración con PostgreSQL")
class JdbcIntegracionTest {

    private final ConexionBD conexion = ConexionBD.desdeEntorno();
    private final BibliotecaPostgres bd = new BibliotecaPostgres(conexion);
    private final Contexto contexto = Contexto.postgres(conexion, Clock.systemDefaultZone());
    private final LocalDate hoy = LocalDate.now();
    private Bibliotecario bibliotecaria;

    @BeforeEach
    void reiniciar() {
        conexion.reiniciarDatosDemo();
        bibliotecaria = contexto.getAcceso().iniciarSesion(DatosDemo.USUARIO, DatosDemo.PASSWORD);
    }

    @Test
    @DisplayName("el hash sembrado en el SQL valida la contraseña de la demo")
    void loginConDatosSembrados() {
        assertEquals("Ana Rojas", bibliotecaria.getNombre());
    }

    @Test
    @DisplayName("ÉXITO: el préstamo queda guardado y se vuelve a leer igual")
    void prestamoPersistido() {
        Prestamo prestamo = contexto.getPrestamos().registrarPrestamo(bibliotecaria,
                DatosDemo.SOCIO_HABILITADO, DatosDemo.EJEMPLAR_DISPONIBLE);

        Prestamo leido = bd.buscarActivoPorEjemplar(DatosDemo.EJEMPLAR_DISPONIBLE).orElseThrow();
        assertEquals(prestamo.getIdPrestamo(), leido.getIdPrestamo());
        assertEquals(hoy.plusDays(Prestamo.DIAS_PLAZO), leido.getFechaDevolucionEsperada());
        assertEquals(EstadoEjemplar.PRESTADO, bd.buscarPorCodigo(DatosDemo.EJEMPLAR_DISPONIBLE).orElseThrow().getEstado());
        Socio socio = bd.buscarPorId(DatosDemo.SOCIO_HABILITADO).orElseThrow();
        assertEquals(1, socio.getPrestamosActivos().size());
    }

    @Test
    @DisplayName("ERROR: los datos sembrados producen los mismos rechazos que en memoria")
    void rechazos() {
        assertThrows(EjemplarNoDisponibleException.class, () -> contexto.getPrestamos()
                .registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_HABILITADO, DatosDemo.EJEMPLAR_PRESTADO));
        assertThrows(EjemplarNoDisponibleException.class, () -> contexto.getPrestamos()
                .registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_HABILITADO, DatosDemo.EJEMPLAR_EN_MANTENIMIENTO));
        assertThrows(SocioInhabilitadoException.class, () -> contexto.getPrestamos()
                .registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_CON_MULTA, "EJ-0003"));
        assertThrows(SocioInhabilitadoException.class, () -> contexto.getPrestamos()
                .registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_CON_VENCIDO, "EJ-0003"));
        assertThrows(RecursoNoEncontradoException.class, () -> contexto.getPrestamos()
                .registrarPrestamo(bibliotecaria, 99, "EJ-0003"));
        assertTrue(bd.buscarPorCodigo("EJ-0003").orElseThrow().estaDisponible(), "ningún rechazo cambió datos");
    }

    @Test
    @DisplayName("DEVOLUCIÓN atrasada: la multa y el bloqueo del socio quedan guardados")
    void devolucionPersistida() {
        Prestamo devuelto = contexto.getPrestamos().registrarDevolucion(bibliotecaria, DatosDemo.EJEMPLAR_PRESTADO);

        assertEquals(DatosDemo.DIAS_ATRASO_SEMBRADO * Prestamo.MULTA_POR_DIA, devuelto.getMulta());
        assertTrue(bd.buscarActivoPorEjemplar(DatosDemo.EJEMPLAR_PRESTADO).isEmpty());
        assertTrue(bd.buscarPorCodigo(DatosDemo.EJEMPLAR_PRESTADO).orElseThrow().estaDisponible());
        Socio socio = bd.buscarPorId(DatosDemo.SOCIO_CON_VENCIDO).orElseThrow();
        assertTrue(socio.isTieneMulta());
        assertTrue(socio.getPrestamosActivos().isEmpty());
        // Repetir la devolución sobre el objeto ya cerrado: la base la rechaza
        assertThrows(RecursoNoEncontradoException.class, () -> bd.registrarDevolucion(devuelto));
    }

    @Test
    @DisplayName("CONCURRENCIA: dos préstamos validados del mismo ejemplar → la base acepta solo uno")
    void dobleRegistroDelMismoEjemplar() {
        // Dos bibliotecarios leen el ejemplar cuando aún está disponible y ambos pasan la validación
        Prestamo primero = bibliotecaria.registrarPrestamo(bd.buscarPorId(1).orElseThrow(),
                bd.buscarPorCodigo("EJ-0005").orElseThrow(), hoy);
        Prestamo segundo = bibliotecaria.registrarPrestamo(bd.buscarPorId(1).orElseThrow(),
                bd.buscarPorCodigo("EJ-0005").orElseThrow(), hoy);

        bd.registrar(primero);
        assertThrows(EjemplarNoDisponibleException.class, () -> bd.registrar(segundo));
        assertEquals(primero.getIdPrestamo(), bd.buscarActivoPorEjemplar("EJ-0005").orElseThrow().getIdPrestamo());
    }

    @Test
    @DisplayName("CONCURRENCIA: 8 hilos piden el mismo ejemplar a la vez → exactamente 1 préstamo")
    void ochoHilosALaVez() throws Exception {
        int hilos = 8;
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(hilos)) {
            for (int i = 0; i < hilos; i++) {
                resultados.add(pool.submit(() -> {
                    largada.await();
                    try {
                        contexto.getPrestamos().registrarPrestamo(bibliotecaria, DatosDemo.SOCIO_HABILITADO, "EJ-0006");
                        return true;
                    } catch (EjemplarNoDisponibleException rechazado) {
                        return false;
                    }
                }));
            }
            largada.countDown();
            int exitos = 0;
            for (Future<Boolean> resultado : resultados) {
                exitos += resultado.get() ? 1 : 0;
            }
            assertEquals(1, exitos);
        }
    }

    @Test
    @DisplayName("el catálogo trae los ejemplares y los títulos sin copias")
    void catalogo() {
        List<Libro> todos = contexto.getCatalogo().buscar("");
        assertEquals(4, todos.size());

        Libro cleanCode = contexto.getCatalogo().buscar("clean").get(0);
        assertEquals(2, cleanCode.getEjemplares().size());
        assertEquals(1, cleanCode.obtenerEjemplaresDisponibles().size());
        assertEquals(0, contexto.getCatalogo().buscar("UML").get(0).getEjemplares().size());
        assertEquals(0, contexto.getCatalogo().buscar("100%").size(), "el % se busca literal, no como comodín");
    }

    @Test
    @DisplayName("lo que no existe se informa vacío, no como error técnico")
    void inexistentes() {
        assertFalse(bd.buscarPorId(99).isPresent());
        assertFalse(bd.buscarPorCodigo("EJ-9999").isPresent());
        assertFalse(bd.buscarPorUsuario("nadie").isPresent());
    }
}
