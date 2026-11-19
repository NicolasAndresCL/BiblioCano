package cl.bibliocano.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.bibliocano.dominio.CredencialesInvalidasException;
import cl.bibliocano.dominio.Libro;
import cl.bibliocano.persistencia.memoria.BibliotecaEnMemoria;
import cl.bibliocano.persistencia.memoria.DatosDemo;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Servicios de acceso y catálogo")
class ServicioAccesoCatalogoTest {

    private final BibliotecaEnMemoria memoria = DatosDemo.crear(LocalDate.of(2026, 11, 26));
    private final ServicioAcceso acceso = new ServicioAcceso(memoria);
    private final ServicioCatalogo catalogo = new ServicioCatalogo(memoria);

    @Test
    @DisplayName("inicia sesión con credenciales válidas (ignora espacios en el usuario)")
    void sesionValida() {
        assertEquals("Ana Rojas", acceso.iniciarSesion("  " + DatosDemo.USUARIO + " ", DatosDemo.PASSWORD).getNombre());
    }

    @Test
    @DisplayName("usuario inexistente y contraseña errónea dan el MISMO mensaje")
    void mismoMensaje() {
        String sinUsuario = assertThrows(CredencialesInvalidasException.class,
                () -> acceso.iniciarSesion("nadie", DatosDemo.PASSWORD)).getMessage();
        String malaClave = assertThrows(CredencialesInvalidasException.class,
                () -> acceso.iniciarSesion(DatosDemo.USUARIO, "mala")).getMessage();
        assertThrows(CredencialesInvalidasException.class, () -> acceso.iniciarSesion(null, null));

        assertEquals(sinUsuario, malaClave);
    }

    @ParameterizedTest(name = "«{0}» encuentra {1} libro(s)")
    @CsvSource({
        "clean,          1",   // título, sin distinguir mayúsculas
        "MARTIN,         2",   // autor: Robert C. Martin y Martin Fowler
        "9780307474728,  1",   // ISBN
        "'',             4",   // vacío: catálogo completo
        "no-existe,      0",
    })
    @DisplayName("busca por título, autor o ISBN")
    void busca(String texto, int cantidad) {
        assertEquals(cantidad, catalogo.buscar(texto).size());
    }

    @Test
    @DisplayName("null equivale a listar todo, y un título sin copias aparece con 0 ejemplares")
    void nullListaTodo() {
        List<Libro> todos = catalogo.buscar(null);

        assertEquals(4, todos.size());
        Libro uml = catalogo.buscar("UML Distilled").get(0);
        assertEquals(0, uml.getEjemplares().size());
    }
}
