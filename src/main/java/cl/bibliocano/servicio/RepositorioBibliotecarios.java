package cl.bibliocano.servicio;

import cl.bibliocano.dominio.Bibliotecario;
import java.util.Optional;

/** Acceso al personal para el inicio de sesión. */
public interface RepositorioBibliotecarios {

    Optional<Bibliotecario> buscarPorUsuario(String usuario);
}
