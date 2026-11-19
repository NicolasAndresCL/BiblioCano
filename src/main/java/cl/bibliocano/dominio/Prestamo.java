package cl.bibliocano.dominio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Registro de que un {@link Socio} se llevó un {@link Ejemplar}, autorizado por un
 * {@link Bibliotecario}. Guarda la trazabilidad que pide el objetivo del sistema: fecha de emisión,
 * fecha límite, ejemplar asignado y estado.
 *
 * <p>Se crea solo desde {@link Bibliotecario#registrarPrestamo} (constructor de paquete), que antes
 * valida las reglas de negocio; así no puede existir un préstamo que se las haya saltado. La
 * persistencia lo reconstruye con {@link #reconstituir}.
 *
 * <p>No redefine {@code equals}: hasta que la base le asigna un id no tiene clave natural, y una
 * igualdad que cambia después de entrar a una colección la corrompe.
 */
public final class Prestamo {

    /** Plazo de préstamo en días corridos. */
    public static final int DIAS_PLAZO = 14;
    /** Multa en pesos por cada día de atraso. Pesos enteros: el peso chileno no usa decimales. */
    public static final int MULTA_POR_DIA = 500;

    private int idPrestamo;
    private final Socio socio;
    private final Ejemplar ejemplar;
    private final Bibliotecario bibliotecario;
    private final LocalDate fechaPrestamo;
    private final LocalDate fechaDevolucionEsperada;
    private LocalDate fechaDevolucionReal;
    private EstadoPrestamo estado;
    private int multa;

    Prestamo(Socio socio, Ejemplar ejemplar, Bibliotecario bibliotecario, LocalDate fechaPrestamo) {
        this(0, socio, ejemplar, bibliotecario, fechaPrestamo,
                Objects.requireNonNull(fechaPrestamo, "fechaPrestamo").plusDays(DIAS_PLAZO),
                null, EstadoPrestamo.ACTIVO, 0);
    }

    private Prestamo(int idPrestamo, Socio socio, Ejemplar ejemplar, Bibliotecario bibliotecario,
            LocalDate fechaPrestamo, LocalDate fechaDevolucionEsperada, LocalDate fechaDevolucionReal,
            EstadoPrestamo estado, int multa) {
        this.idPrestamo = idPrestamo;
        this.socio = Objects.requireNonNull(socio, "socio");
        this.ejemplar = Objects.requireNonNull(ejemplar, "ejemplar");
        this.bibliotecario = Objects.requireNonNull(bibliotecario, "bibliotecario");
        this.fechaPrestamo = Objects.requireNonNull(fechaPrestamo, "fechaPrestamo");
        this.fechaDevolucionEsperada = Objects.requireNonNull(fechaDevolucionEsperada, "fechaDevolucionEsperada");
        this.fechaDevolucionReal = fechaDevolucionReal;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.multa = multa;
        if (estado == EstadoPrestamo.ACTIVO) {
            socio.agregarPrestamo(this);
        }
    }

    /** Reconstruye un préstamo leído de la base de datos, sin volver a aplicar las reglas de creación. */
    public static Prestamo reconstituir(int idPrestamo, Socio socio, Ejemplar ejemplar,
            Bibliotecario bibliotecario, LocalDate fechaPrestamo, LocalDate fechaDevolucionEsperada,
            LocalDate fechaDevolucionReal, EstadoPrestamo estado, int multa) {
        return new Prestamo(idPrestamo, socio, ejemplar, bibliotecario, fechaPrestamo,
                fechaDevolucionEsperada, fechaDevolucionReal, estado, multa);
    }

    /** Multa que correspondería si el ejemplar se entrega en {@code fechaEntrega}; 0 si llega a tiempo. */
    public int calcularMulta(LocalDate fechaEntrega) {
        long diasAtraso = ChronoUnit.DAYS.between(fechaDevolucionEsperada, fechaEntrega);
        return diasAtraso > 0 ? Math.toIntExact(diasAtraso * MULTA_POR_DIA) : 0;
    }

    /** Vencido = sigue activo y la fecha límite ya pasó. Es una condición, no un estado guardado. */
    public boolean estaVencido(LocalDate hoy) {
        return estado == EstadoPrestamo.ACTIVO && hoy.isAfter(fechaDevolucionEsperada);
    }

    /**
     * Cierra el préstamo: fija la fecha real y la multa, libera el ejemplar y, si hubo atraso,
     * deja al socio con multa impaga.
     *
     * @throws IllegalStateException si el préstamo ya estaba devuelto
     */
    public void finalizarPrestamo(LocalDate fechaEntrega) {
        if (estado != EstadoPrestamo.ACTIVO) {
            throw new IllegalStateException("El préstamo " + idPrestamo + " ya fue devuelto");
        }
        if (fechaEntrega.isBefore(fechaPrestamo)) {
            throw new IllegalArgumentException("La devolución no puede ser anterior al préstamo");
        }
        this.fechaDevolucionReal = fechaEntrega;
        this.multa = calcularMulta(fechaEntrega);
        this.estado = EstadoPrestamo.DEVUELTO;
        ejemplar.cambiarEstado(EstadoEjemplar.DISPONIBLE);
        socio.quitarPrestamo(this);
        if (multa > 0) {
            socio.registrarMulta();
        }
    }

    /** La persistencia asigna el id generado por la base; solo se permite una vez. */
    public void asignarId(int id) {
        if (this.idPrestamo != 0) {
            throw new IllegalStateException("El préstamo ya tiene id " + this.idPrestamo);
        }
        this.idPrestamo = id;
    }

    public int getIdPrestamo() {
        return idPrestamo;
    }

    public Socio getSocio() {
        return socio;
    }

    public Ejemplar getEjemplar() {
        return ejemplar;
    }

    public Bibliotecario getBibliotecario() {
        return bibliotecario;
    }

    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    public LocalDate getFechaDevolucionEsperada() {
        return fechaDevolucionEsperada;
    }

    /** {@code null} mientras el préstamo está activo. */
    public LocalDate getFechaDevolucionReal() {
        return fechaDevolucionReal;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public int getMulta() {
        return multa;
    }

    @Override
    public String toString() {
        return "Préstamo " + idPrestamo + ": " + ejemplar.getCodigoBarra() + " a " + socio.getNombre()
                + " hasta " + fechaDevolucionEsperada + " (" + estado + ")";
    }
}
