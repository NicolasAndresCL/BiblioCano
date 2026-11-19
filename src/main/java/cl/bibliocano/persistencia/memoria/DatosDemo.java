package cl.bibliocano.persistencia.memoria;

import cl.bibliocano.dominio.Bibliotecario;
import cl.bibliocano.dominio.Contrasenas;
import cl.bibliocano.dominio.Ejemplar;
import cl.bibliocano.dominio.EstadoEjemplar;
import cl.bibliocano.dominio.EstadoPrestamo;
import cl.bibliocano.dominio.Libro;
import cl.bibliocano.dominio.Prestamo;
import cl.bibliocano.dominio.Socio;
import java.time.LocalDate;

/**
 * Datos de ejemplo, los mismos que {@code db/datos-demo.sql} carga en PostgreSQL. Cada socio y cada
 * ejemplar existe para mostrar un escenario de la demostración: si se cambia uno aquí, se cambia
 * también en el SQL (la prueba de integración {@code JdbcIntegracionTest} recorre los mismos
 * escenarios contra la base).
 */
public final class DatosDemo {

    public static final String USUARIO = "bibliotecaria";
    public static final String PASSWORD = "biblio2026";

    /** Sin multas ni préstamos: el camino feliz. */
    public static final int SOCIO_HABILITADO = 1;
    /** Con multa impaga: préstamo rechazado. */
    public static final int SOCIO_CON_MULTA = 2;
    /** Con un préstamo vencido hace 6 días: préstamo rechazado y devolución con multa. */
    public static final int SOCIO_CON_VENCIDO = 3;

    public static final String EJEMPLAR_DISPONIBLE = "EJ-0001";
    public static final String EJEMPLAR_PRESTADO = "EJ-0002";
    public static final String EJEMPLAR_EN_MANTENIMIENTO = "EJ-0004";

    /** Días de atraso del préstamo vencido sembrado: 6 días × $500 = $3.000 de multa. */
    public static final int DIAS_ATRASO_SEMBRADO = 6;

    private DatosDemo() {
    }

    /** Crea un repositorio en memoria con los datos de ejemplo, con fechas relativas a {@code hoy}. */
    public static BibliotecaEnMemoria crear(LocalDate hoy) {
        BibliotecaEnMemoria memoria = new BibliotecaEnMemoria();

        Bibliotecario bibliotecaria = new Bibliotecario(1, "Ana Rojas", USUARIO, Contrasenas.generarHash(PASSWORD));
        memoria.agregarBibliotecario(bibliotecaria);

        Libro cleanCode = new Libro("9780132350884", "Clean Code", "Robert C. Martin", "Ingeniería de software");
        cleanCode.agregarEjemplar(EJEMPLAR_DISPONIBLE, "Estante A-1");
        Ejemplar prestado = cleanCode.agregarEjemplar(EJEMPLAR_PRESTADO, "Estante A-1");
        Libro patrones = new Libro("9780201633610", "Design Patterns", "Gamma, Helm, Johnson y Vlissides",
                "Ingeniería de software");
        patrones.agregarEjemplar("EJ-0003", "Estante A-2");
        patrones.agregarEjemplar(EJEMPLAR_EN_MANTENIMIENTO, "Bodega").cambiarEstado(EstadoEjemplar.MANTENIMIENTO);
        Libro cienAnios = new Libro("9780307474728", "Cien años de soledad", "Gabriel García Márquez", "Novela");
        cienAnios.agregarEjemplar("EJ-0005", "Estante C-4");
        cienAnios.agregarEjemplar("EJ-0006", "Estante C-4");
        Libro uml = new Libro("9780321193681", "UML Distilled", "Martin Fowler", "Ingeniería de software");
        memoria.agregarLibro(cleanCode);
        memoria.agregarLibro(patrones);
        memoria.agregarLibro(cienAnios);
        memoria.agregarLibro(uml); // título sin copias: muestra la multiplicidad 0..* de la composición

        Socio camila = new Socio(SOCIO_HABILITADO, "12.345.678-5", "Camila Fuentes", "camila@correo.cl", false);
        Socio diego = new Socio(SOCIO_CON_MULTA, "9.876.543-3", "Diego Morales", "diego@correo.cl", true);
        Socio valentina = new Socio(SOCIO_CON_VENCIDO, "15.222.333-K", "Valentina Soto", "valentina@correo.cl", false);
        memoria.agregarSocio(camila);
        memoria.agregarSocio(diego);
        memoria.agregarSocio(valentina);

        // Préstamo vencido: se prestó hace 20 días con plazo de 14, así que lleva 6 de atraso
        LocalDate fechaPrestamo = hoy.minusDays(Prestamo.DIAS_PLAZO + DIAS_ATRASO_SEMBRADO);
        prestado.cambiarEstado(EstadoEjemplar.PRESTADO);
        memoria.agregarPrestamoExistente(Prestamo.reconstituir(1, valentina, prestado, bibliotecaria, fechaPrestamo,
                fechaPrestamo.plusDays(Prestamo.DIAS_PLAZO), null, EstadoPrestamo.ACTIVO, 0));
        return memoria;
    }
}
