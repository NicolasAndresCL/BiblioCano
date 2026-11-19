package cl.bibliocano.dominio;

/** El socio tiene una multa impaga o un préstamo vencido: no puede recibir otro libro. */
public final class SocioInhabilitadoException extends BibliotecaException {

    private static final long serialVersionUID = 1L;

    public SocioInhabilitadoException(String mensaje) {
        super(mensaje);
    }

    public SocioInhabilitadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
