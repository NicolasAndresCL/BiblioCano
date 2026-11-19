package cl.bibliocano.dominio;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Funcionario que opera el sistema y autoriza préstamos y devoluciones.
 *
 * <p>Es el creador de {@link Prestamo} (patrón Creador de GRASP: es quien registra el préstamo y
 * tiene los datos para iniciarlo). Antes de crearlo aplica las dos reglas que el sistema automatiza:
 * socio habilitado y ejemplar disponible. Guarda el hash de la contraseña, nunca la contraseña.
 */
public class Bibliotecario {

    private final int idBibliotecario;
    private final String nombre;
    private final String usuario;
    private final String hashPassword;

    public Bibliotecario(int idBibliotecario, String nombre, String usuario, String hashPassword) {
        if (idBibliotecario <= 0) {
            throw new IllegalArgumentException("El id del bibliotecario debe ser positivo");
        }
        this.idBibliotecario = idBibliotecario;
        this.nombre = Libro.textoObligatorio(nombre, "nombre");
        this.usuario = Libro.textoObligatorio(usuario, "usuario");
        this.hashPassword = Libro.textoObligatorio(hashPassword, "hash de la contraseña");
    }

    /** {@code true} si la contraseña corresponde al hash guardado. */
    public boolean autenticar(String password) {
        return Contrasenas.coincide(password, hashPassword);
    }

    /**
     * Funcionalidad central: presta {@code ejemplar} a {@code socio} con fecha {@code hoy}.
     *
     * <p>Orden de las validaciones (el mismo de los diagramas de secuencia, actividades y
     * comunicación): primero el socio, después el ejemplar.
     *
     * @throws SocioInhabilitadoException     si el socio tiene multa impaga o un préstamo vencido
     * @throws EjemplarNoDisponibleException si el ejemplar está prestado o en mantenimiento
     */
    public Prestamo registrarPrestamo(Socio socio, Ejemplar ejemplar, LocalDate hoy) {
        Objects.requireNonNull(socio, "socio");
        Objects.requireNonNull(ejemplar, "ejemplar");
        if (!socio.validarEstado(hoy)) {
            String motivo = socio.isTieneMulta() ? "tiene una multa impaga" : "tiene un préstamo vencido";
            throw new SocioInhabilitadoException("El socio " + socio.getNombre() + " " + motivo
                    + ": no puede recibir otro préstamo");
        }
        if (!ejemplar.estaDisponible()) {
            throw new EjemplarNoDisponibleException("El ejemplar " + ejemplar.getCodigoBarra()
                    + " no está disponible (estado " + ejemplar.getEstado() + ")");
        }
        Prestamo prestamo = new Prestamo(socio, ejemplar, this, hoy);
        ejemplar.cambiarEstado(EstadoEjemplar.PRESTADO);
        return prestamo;
    }

    /**
     * Recibe el ejemplar de vuelta y cierra el préstamo.
     *
     * @return la multa cobrada en pesos (0 si llegó a tiempo)
     */
    public int registrarDevolucion(Prestamo prestamo, LocalDate hoy) {
        Objects.requireNonNull(prestamo, "prestamo").finalizarPrestamo(hoy);
        return prestamo.getMulta();
    }

    public int getIdBibliotecario() {
        return idBibliotecario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getHashPassword() {
        return hashPassword;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Bibliotecario b && idBibliotecario == b.idBibliotecario;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idBibliotecario);
    }

    /** No incluye el hash: un {@code toString} termina en logs y en pantallas. */
    @Override
    public String toString() {
        return nombre + " (" + usuario + ")";
    }
}
