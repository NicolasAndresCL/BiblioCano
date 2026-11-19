package cl.bibliocano.persistencia.memoria;

import cl.bibliocano.dominio.Bibliotecario;
import cl.bibliocano.dominio.Ejemplar;
import cl.bibliocano.dominio.EjemplarNoDisponibleException;
import cl.bibliocano.dominio.EstadoPrestamo;
import cl.bibliocano.dominio.Libro;
import cl.bibliocano.dominio.Prestamo;
import cl.bibliocano.dominio.Socio;
import cl.bibliocano.servicio.RepositorioBibliotecarios;
import cl.bibliocano.servicio.RepositorioEjemplares;
import cl.bibliocano.servicio.RepositorioLibros;
import cl.bibliocano.servicio.RepositorioPrestamos;
import cl.bibliocano.servicio.RepositorioSocios;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación en memoria de los cinco repositorios.
 *
 * <p>Sirve para dos cosas: las pruebas unitarias de las reglas de negocio (rápidas, sin base de
 * datos) y el modo {@code memoria} de la aplicación, que permite mostrar el sistema aunque Docker no
 * esté disponible. Los datos se pierden al cerrar el programa.
 */
public class BibliotecaEnMemoria implements RepositorioSocios, RepositorioEjemplares,
        RepositorioPrestamos, RepositorioLibros, RepositorioBibliotecarios {

    private final Map<String, Libro> libros = new LinkedHashMap<>();
    private final Map<String, Ejemplar> ejemplares = new LinkedHashMap<>();
    private final Map<Integer, Socio> socios = new LinkedHashMap<>();
    private final Map<String, Bibliotecario> bibliotecarios = new LinkedHashMap<>();
    private final List<Prestamo> prestamos = new ArrayList<>();
    private int siguienteIdPrestamo = 1;

    // ----------------------------------------------------------------- carga de datos

    /** Registra el libro y todas sus copias. */
    public void agregarLibro(Libro libro) {
        libros.put(libro.getIsbn(), libro);
        for (Ejemplar ejemplar : libro.getEjemplares()) {
            ejemplares.put(ejemplar.getCodigoBarra(), ejemplar);
        }
    }

    public void agregarSocio(Socio socio) {
        socios.put(socio.getIdSocio(), socio);
    }

    public void agregarBibliotecario(Bibliotecario bibliotecario) {
        bibliotecarios.put(bibliotecario.getUsuario(), bibliotecario);
    }

    /** Agrega un préstamo ya existente (datos de ejemplo), respetando su id. */
    public void agregarPrestamoExistente(Prestamo prestamo) {
        prestamos.add(prestamo);
        siguienteIdPrestamo = Math.max(siguienteIdPrestamo, prestamo.getIdPrestamo() + 1);
    }

    // ----------------------------------------------------------------- repositorios

    @Override
    public Optional<Socio> buscarPorId(int idSocio) {
        return Optional.ofNullable(socios.get(idSocio));
    }

    @Override
    public Optional<Ejemplar> buscarPorCodigo(String codigoBarra) {
        return Optional.ofNullable(ejemplares.get(codigoBarra));
    }

    @Override
    public Optional<Bibliotecario> buscarPorUsuario(String usuario) {
        return Optional.ofNullable(bibliotecarios.get(usuario));
    }

    @Override
    public List<Libro> buscar(String texto) {
        String buscado = texto.toLowerCase(Locale.ROOT);
        List<Libro> encontrados = new ArrayList<>();
        for (Libro libro : libros.values()) {
            if (libro.getTitulo().toLowerCase(Locale.ROOT).contains(buscado)
                    || libro.getAutor().toLowerCase(Locale.ROOT).contains(buscado)
                    || libro.getIsbn().contains(buscado)) {
                encontrados.add(libro);
            }
        }
        return encontrados;
    }

    /** Misma garantía que el índice único de PostgreSQL: un solo préstamo activo por ejemplar. */
    @Override
    public void registrar(Prestamo prestamo) {
        String codigo = prestamo.getEjemplar().getCodigoBarra();
        if (buscarActivoPorEjemplar(codigo).isPresent()) {
            throw new EjemplarNoDisponibleException("El ejemplar " + codigo + " ya tiene un préstamo activo");
        }
        prestamo.asignarId(siguienteIdPrestamo++);
        prestamos.add(prestamo);
    }

    @Override
    public Optional<Prestamo> buscarActivoPorEjemplar(String codigoBarra) {
        for (Prestamo prestamo : prestamos) {
            if (prestamo.getEstado() == EstadoPrestamo.ACTIVO
                    && prestamo.getEjemplar().getCodigoBarra().equals(codigoBarra)) {
                return Optional.of(prestamo);
            }
        }
        return Optional.empty();
    }

    /** En memoria los objetos ya quedaron modificados; solo se comprueba que el préstamo exista. */
    @Override
    public void registrarDevolucion(Prestamo prestamo) {
        if (!prestamos.contains(prestamo)) {
            throw new IllegalArgumentException("El préstamo " + prestamo.getIdPrestamo() + " no está registrado");
        }
    }

    /** Todos los préstamos, activos y devueltos (para pruebas y para la demo). */
    public List<Prestamo> listarPrestamos() {
        return List.copyOf(prestamos);
    }
}
