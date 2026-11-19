package cl.bibliocano.persistencia.jdbc;

import cl.bibliocano.dominio.Bibliotecario;
import cl.bibliocano.dominio.Ejemplar;
import cl.bibliocano.dominio.EjemplarNoDisponibleException;
import cl.bibliocano.dominio.EstadoEjemplar;
import cl.bibliocano.dominio.EstadoPrestamo;
import cl.bibliocano.dominio.Libro;
import cl.bibliocano.dominio.Prestamo;
import cl.bibliocano.dominio.RecursoNoEncontradoException;
import cl.bibliocano.dominio.Socio;
import cl.bibliocano.servicio.RepositorioBibliotecarios;
import cl.bibliocano.servicio.RepositorioEjemplares;
import cl.bibliocano.servicio.RepositorioLibros;
import cl.bibliocano.servicio.RepositorioPrestamos;
import cl.bibliocano.servicio.RepositorioSocios;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación PostgreSQL de los cinco repositorios, con JDBC y SQL explícito.
 *
 * <p>Cada operación abre su conexión y la cierra al terminar (try-with-resources). Las que
 * escriben en más de una tabla usan una transacción: o se guarda todo, o nada.
 *
 * <p>Los objetos que devuelve son agregados parciales: un {@link Ejemplar} trae su {@link Libro},
 * pero ese libro no trae sus otras copias. Basta para los casos de uso implementados.
 */
public class BibliotecaPostgres implements RepositorioSocios, RepositorioEjemplares,
        RepositorioPrestamos, RepositorioLibros, RepositorioBibliotecarios {

    /** Código SQLSTATE de PostgreSQL para «violación de restricción única». */
    private static final String VIOLACION_UNICA = "23505";

    private static final String SELECT_PRESTAMO = """
            SELECT p.id_prestamo, p.fecha_prestamo, p.fecha_devolucion_esperada, p.fecha_devolucion_real,
                   p.estado AS estado_prestamo, p.multa,
                   s.id_socio, s.rut, s.nombre AS nombre_socio, s.correo, s.tiene_multa,
                   e.codigo_barra, e.ubicacion, e.estado AS estado_ejemplar,
                   l.isbn, l.titulo, l.autor, l.categoria,
                   b.id_bibliotecario, b.nombre AS nombre_bibliotecario, b.usuario, b.password_hash
              FROM prestamo p
              JOIN socio s         ON s.id_socio = p.id_socio
              JOIN ejemplar e      ON e.codigo_barra = p.codigo_barra
              JOIN libro l         ON l.isbn = e.isbn
              JOIN bibliotecario b ON b.id_bibliotecario = p.id_bibliotecario
            """;

    private final ConexionBD conexionBD;

    public BibliotecaPostgres(ConexionBD conexionBD) {
        this.conexionBD = Objects.requireNonNull(conexionBD, "conexionBD");
    }

    // ----------------------------------------------------------------- socios

    @Override
    public Optional<Socio> buscarPorId(int idSocio) {
        try (Connection c = conexionBD.abrir()) {
            Socio socio;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_socio, rut, nombre AS nombre_socio, correo, tiene_multa FROM socio WHERE id_socio = ?")) {
                ps.setInt(1, idSocio);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    socio = socio(rs);
                }
            }
            // Préstamos activos: los necesita Socio.validarEstado para detectar vencidos
            try (PreparedStatement ps = c.prepareStatement(
                    SELECT_PRESTAMO + " WHERE p.id_socio = ? AND p.estado = 'ACTIVO'")) {
                ps.setInt(1, idSocio);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        prestamo(rs, socio); // se agrega solo a la lista del socio
                    }
                }
            }
            return Optional.of(socio);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo leer el socio " + idSocio, e);
        }
    }

    // ----------------------------------------------------------------- ejemplares y libros

    @Override
    public Optional<Ejemplar> buscarPorCodigo(String codigoBarra) {
        String sql = """
                SELECT e.codigo_barra, e.ubicacion, e.estado AS estado_ejemplar,
                       l.isbn, l.titulo, l.autor, l.categoria
                  FROM ejemplar e JOIN libro l ON l.isbn = e.isbn
                 WHERE e.codigo_barra = ?
                """;
        try (Connection c = conexionBD.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codigoBarra);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(ejemplar(rs, libro(rs))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo leer el ejemplar " + codigoBarra, e);
        }
    }

    @Override
    public List<Libro> buscar(String texto) {
        String sql = """
                SELECT l.isbn, l.titulo, l.autor, l.categoria,
                       e.codigo_barra, e.ubicacion, e.estado AS estado_ejemplar
                  FROM libro l LEFT JOIN ejemplar e ON e.isbn = l.isbn
                 WHERE l.titulo ILIKE ? OR l.autor ILIKE ? OR l.isbn ILIKE ?
                 ORDER BY l.titulo, e.codigo_barra
                """;
        // % y _ son comodines de LIKE: se escapan para buscar el texto literal
        String patron = "%" + texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        try (Connection c = conexionBD.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            Map<String, Libro> libros = new LinkedHashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Libro libro = libros.get(rs.getString("isbn"));
                    if (libro == null) {
                        libro = libro(rs);
                        libros.put(libro.getIsbn(), libro);
                    }
                    if (rs.getString("codigo_barra") != null) { // LEFT JOIN: título sin copias
                        ejemplar(rs, libro);
                    }
                }
            }
            return new ArrayList<>(libros.values());
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el catálogo", e);
        }
    }

    // ----------------------------------------------------------------- bibliotecarios

    @Override
    public Optional<Bibliotecario> buscarPorUsuario(String usuario) {
        String sql = """
                SELECT id_bibliotecario, nombre AS nombre_bibliotecario, usuario, password_hash
                  FROM bibliotecario WHERE usuario = ?
                """;
        try (Connection c = conexionBD.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(bibliotecario(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo leer el bibliotecario " + usuario, e);
        }
    }

    // ----------------------------------------------------------------- préstamos

    /**
     * Transacción de dos pasos: marcar el ejemplar como prestado SOLO si sigue disponible, e
     * insertar el préstamo. Si otro bibliotecario lo prestó entre la validación y este momento, el
     * {@code UPDATE ... AND estado = 'DISPONIBLE'} no afecta filas (o el índice único rechaza el
     * INSERT) y se informa como ejemplar no disponible, sin dejar datos a medias.
     */
    @Override
    public void registrar(Prestamo prestamo) {
        String codigo = prestamo.getEjemplar().getCodigoBarra();
        try (Connection c = conexionBD.abrir()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE ejemplar SET estado = 'PRESTADO' WHERE codigo_barra = ? AND estado = 'DISPONIBLE'")) {
                    ps.setString(1, codigo);
                    if (ps.executeUpdate() == 0) {
                        c.rollback();
                        throw new EjemplarNoDisponibleException(
                                "El ejemplar " + codigo + " acaba de ser prestado o retirado por otra operación");
                    }
                }
                int id;
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO prestamo (id_bibliotecario, id_socio, codigo_barra, fecha_prestamo,
                                              fecha_devolucion_esperada, estado, multa)
                        VALUES (?, ?, ?, ?, ?, 'ACTIVO', 0)
                        RETURNING id_prestamo
                        """)) {
                    ps.setInt(1, prestamo.getBibliotecario().getIdBibliotecario());
                    ps.setInt(2, prestamo.getSocio().getIdSocio());
                    ps.setString(3, codigo);
                    ps.setObject(4, prestamo.getFechaPrestamo());
                    ps.setObject(5, prestamo.getFechaDevolucionEsperada());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        id = rs.getInt(1);
                    }
                }
                c.commit();
                prestamo.asignarId(id);
            } catch (SQLException e) {
                c.rollback();
                if (VIOLACION_UNICA.equals(e.getSQLState())) {
                    throw new EjemplarNoDisponibleException("El ejemplar " + codigo + " ya tiene un préstamo activo", e);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo registrar el préstamo de " + codigo, e);
        }
    }

    @Override
    public Optional<Prestamo> buscarActivoPorEjemplar(String codigoBarra) {
        try (Connection c = conexionBD.abrir(); PreparedStatement ps = c.prepareStatement(
                SELECT_PRESTAMO + " WHERE p.codigo_barra = ? AND p.estado = 'ACTIVO'")) {
            ps.setString(1, codigoBarra);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(prestamo(rs, socio(rs))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo leer el préstamo del ejemplar " + codigoBarra, e);
        }
    }

    /** Transacción de hasta tres pasos: cerrar el préstamo, liberar el ejemplar y anotar la multa. */
    @Override
    public void registrarDevolucion(Prestamo prestamo) {
        try (Connection c = conexionBD.abrir()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("""
                        UPDATE prestamo SET estado = 'DEVUELTO', fecha_devolucion_real = ?, multa = ?
                         WHERE id_prestamo = ? AND estado = 'ACTIVO'
                        """)) {
                    ps.setObject(1, prestamo.getFechaDevolucionReal());
                    ps.setInt(2, prestamo.getMulta());
                    ps.setInt(3, prestamo.getIdPrestamo());
                    if (ps.executeUpdate() == 0) {
                        c.rollback();
                        throw new RecursoNoEncontradoException(
                                "El préstamo " + prestamo.getIdPrestamo() + " ya había sido devuelto");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE ejemplar SET estado = 'DISPONIBLE' WHERE codigo_barra = ?")) {
                    ps.setString(1, prestamo.getEjemplar().getCodigoBarra());
                    ps.executeUpdate();
                }
                if (prestamo.getMulta() > 0) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE socio SET tiene_multa = TRUE WHERE id_socio = ?")) {
                        ps.setInt(1, prestamo.getSocio().getIdSocio());
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo registrar la devolución " + prestamo.getIdPrestamo(), e);
        }
    }

    // ----------------------------------------------------------------- mapeo fila → objeto

    private static Socio socio(ResultSet rs) throws SQLException {
        return new Socio(rs.getInt("id_socio"), rs.getString("rut"), rs.getString("nombre_socio"),
                rs.getString("correo"), rs.getBoolean("tiene_multa"));
    }

    private static Libro libro(ResultSet rs) throws SQLException {
        return new Libro(rs.getString("isbn"), rs.getString("titulo"), rs.getString("autor"),
                rs.getString("categoria"));
    }

    private static Ejemplar ejemplar(ResultSet rs, Libro libro) throws SQLException {
        Ejemplar ejemplar = libro.agregarEjemplar(rs.getString("codigo_barra"), rs.getString("ubicacion"));
        ejemplar.cambiarEstado(EstadoEjemplar.valueOf(rs.getString("estado_ejemplar")));
        return ejemplar;
    }

    private static Bibliotecario bibliotecario(ResultSet rs) throws SQLException {
        return new Bibliotecario(rs.getInt("id_bibliotecario"), rs.getString("nombre_bibliotecario"),
                rs.getString("usuario"), rs.getString("password_hash"));
    }

    private static Prestamo prestamo(ResultSet rs, Socio socio) throws SQLException {
        return Prestamo.reconstituir(rs.getInt("id_prestamo"), socio, ejemplar(rs, libro(rs)), bibliotecario(rs),
                rs.getObject("fecha_prestamo", LocalDate.class),
                rs.getObject("fecha_devolucion_esperada", LocalDate.class),
                rs.getObject("fecha_devolucion_real", LocalDate.class),
                EstadoPrestamo.valueOf(rs.getString("estado_prestamo")), rs.getInt("multa"));
    }
}
