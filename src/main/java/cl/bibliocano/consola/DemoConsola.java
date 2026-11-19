package cl.bibliocano.consola;

import cl.bibliocano.Contexto;
import cl.bibliocano.Version;
import cl.bibliocano.dominio.Bibliotecario;
import cl.bibliocano.dominio.BibliotecaException;
import cl.bibliocano.dominio.CredencialesInvalidasException;
import cl.bibliocano.dominio.EjemplarNoDisponibleException;
import cl.bibliocano.dominio.Libro;
import cl.bibliocano.dominio.Prestamo;
import cl.bibliocano.dominio.RecursoNoEncontradoException;
import cl.bibliocano.dominio.SocioInhabilitadoException;
import cl.bibliocano.persistencia.memoria.DatosDemo;
import java.io.PrintStream;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Demostración por consola de la funcionalidad implementada. Recorre los escenarios de la sección
 * «Pruebas» del informe: el préstamo exitoso y cada camino alternativo o de error, mostrando qué
 * esperaba el sistema y qué ocurrió.
 *
 * <p>Es presentación: no tiene reglas de negocio, solo llama a los servicios e imprime.
 */
public class DemoConsola {

    private static final Locale CHILE = Locale.of("es", "CL");

    private final Contexto contexto;
    private final PrintStream salida;
    private int numero;
    private int fallos;

    public DemoConsola(Contexto contexto, PrintStream salida) {
        this.contexto = contexto;
        this.salida = salida;
    }

    /** @return cuántos escenarios no terminaron como se esperaba (0 = todo correcto) */
    public int ejecutar() {
        salida.println("==============================================================");
        salida.println(" BiblioCano " + Version.actual() + " · demostración por consola");
        salida.println(" Persistencia: " + contexto.getDescripcion());
        salida.println("==============================================================");

        esperarError("Iniciar sesión con contraseña incorrecta", CredencialesInvalidasException.class,
                () -> contexto.getAcceso().iniciarSesion(DatosDemo.USUARIO, "otra-clave"));
        Bibliotecario b = contexto.getAcceso().iniciarSesion(DatosDemo.USUARIO, DatosDemo.PASSWORD);
        esperarExito("Iniciar sesión con credenciales válidas", () -> "Sesión iniciada: " + b);

        esperarExito("Consultar catálogo: «clean»", () -> {
            StringBuilder texto = new StringBuilder();
            for (Libro libro : contexto.getCatalogo().buscar("clean")) {
                texto.append(libro.getTitulo()).append(": ")
                        .append(libro.obtenerEjemplaresDisponibles().size()).append(" de ")
                        .append(libro.getEjemplares().size()).append(" ejemplares disponibles");
            }
            return texto.toString();
        });

        salida.println("\n--- Registrar préstamo (funcionalidad central) ---");
        esperarExito("Préstamo a socio habilitado de ejemplar disponible", () -> {
            Prestamo p = contexto.getPrestamos().registrarPrestamo(b, DatosDemo.SOCIO_HABILITADO,
                    DatosDemo.EJEMPLAR_DISPONIBLE);
            return "Préstamo N.º " + p.getIdPrestamo() + ": " + p.getEjemplar().getLibro().getTitulo()
                    + " a " + p.getSocio().getNombre() + ", devolver a más tardar el "
                    + p.getFechaDevolucionEsperada();
        });
        esperarError("Prestar un ejemplar que ya está prestado", EjemplarNoDisponibleException.class,
                () -> contexto.getPrestamos().registrarPrestamo(b, DatosDemo.SOCIO_HABILITADO,
                        DatosDemo.EJEMPLAR_PRESTADO));
        esperarError("Prestar un ejemplar en mantenimiento", EjemplarNoDisponibleException.class,
                () -> contexto.getPrestamos().registrarPrestamo(b, DatosDemo.SOCIO_HABILITADO,
                        DatosDemo.EJEMPLAR_EN_MANTENIMIENTO));
        esperarError("Prestar a un socio con multa impaga", SocioInhabilitadoException.class,
                () -> contexto.getPrestamos().registrarPrestamo(b, DatosDemo.SOCIO_CON_MULTA, "EJ-0003"));
        esperarError("Prestar a un socio con un préstamo vencido", SocioInhabilitadoException.class,
                () -> contexto.getPrestamos().registrarPrestamo(b, DatosDemo.SOCIO_CON_VENCIDO, "EJ-0003"));
        esperarError("Prestar a un socio que no existe", RecursoNoEncontradoException.class,
                () -> contexto.getPrestamos().registrarPrestamo(b, 99, "EJ-0003"));

        salida.println("\n--- Registrar devolución (extiende: calcular multa) ---");
        esperarExito("Devolución con " + DatosDemo.DIAS_ATRASO_SEMBRADO + " días de atraso", () -> {
            Prestamo p = contexto.getPrestamos().registrarDevolucion(b, DatosDemo.EJEMPLAR_PRESTADO);
            return "Multa de " + pesos(p.getMulta()) + "; " + p.getSocio().getNombre()
                    + (p.getSocio().isTieneMulta() ? " queda con multa impaga" : " sin multa");
        });
        esperarExito("Devolución dentro del plazo", () -> {
            Prestamo p = contexto.getPrestamos().registrarDevolucion(b, DatosDemo.EJEMPLAR_DISPONIBLE);
            return "Multa de " + pesos(p.getMulta()) + "; el ejemplar vuelve a estar "
                    + p.getEjemplar().getEstado();
        });
        esperarError("Devolver un ejemplar sin préstamo activo", RecursoNoEncontradoException.class,
                () -> contexto.getPrestamos().registrarDevolucion(b, "EJ-0003"));

        salida.println("\n==============================================================");
        salida.println(fallos == 0
                ? " Resultado: los " + numero + " escenarios terminaron como se esperaba."
                : " Resultado: " + fallos + " de " + numero + " escenarios NO terminaron como se esperaba.");
        salida.println("==============================================================");
        return fallos;
    }

    private void esperarExito(String titulo, Supplier<String> accion) {
        encabezado(titulo, "éxito");
        try {
            salida.println("   [OK]    " + accion.get());
        } catch (BibliotecaException e) {
            fallos++;
            salida.println("   [FALLA] se rechazó: " + e.getMessage());
        }
    }

    private void esperarError(String titulo, Class<? extends BibliotecaException> esperado, Runnable accion) {
        encabezado(titulo, esperado.getSimpleName());
        try {
            accion.run();
            fallos++;
            salida.println("   [FALLA] el sistema lo permitió");
        } catch (BibliotecaException e) {
            boolean correcto = esperado.isInstance(e);
            if (!correcto) {
                fallos++;
            }
            salida.println((correcto ? "   [OK]    rechazado: " : "   [FALLA] otro error: ") + e.getMessage());
        }
    }

    private void encabezado(String titulo, String esperado) {
        numero++;
        salida.printf("%n%2d. %s%n    esperado: %s%n", numero, titulo, esperado);
    }

    static String pesos(int monto) {
        return "$" + String.format(CHILE, "%,d", monto);
    }
}
