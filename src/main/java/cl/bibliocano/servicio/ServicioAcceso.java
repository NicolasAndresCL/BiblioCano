package cl.bibliocano.servicio;

import cl.bibliocano.dominio.Bibliotecario;
import cl.bibliocano.dominio.CredencialesInvalidasException;
import java.util.Objects;

/** Caso de uso «Iniciar sesión». */
public class ServicioAcceso {

    private static final String MENSAJE = "Usuario o contraseña incorrectos";

    private final RepositorioBibliotecarios bibliotecarios;

    public ServicioAcceso(RepositorioBibliotecarios bibliotecarios) {
        this.bibliotecarios = Objects.requireNonNull(bibliotecarios, "bibliotecarios");
    }

    /**
     * @return el bibliotecario autenticado, que luego firma cada préstamo
     * @throws CredencialesInvalidasException con el mismo mensaje si el usuario no existe o si la
     *                                        contraseña no coincide
     */
    public Bibliotecario iniciarSesion(String usuario, String password) {
        Bibliotecario bibliotecario = bibliotecarios.buscarPorUsuario(usuario == null ? "" : usuario.strip())
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE));
        if (!bibliotecario.autenticar(password)) {
            throw new CredencialesInvalidasException(MENSAJE);
        }
        return bibliotecario;
    }
}
