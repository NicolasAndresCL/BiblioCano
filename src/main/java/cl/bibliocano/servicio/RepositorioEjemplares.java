package cl.bibliocano.servicio;

import cl.bibliocano.dominio.Ejemplar;
import java.util.Optional;

/** Acceso a las copias físicas por su código de barras. */
public interface RepositorioEjemplares {

    /** Devuelve el ejemplar junto con su libro. */
    Optional<Ejemplar> buscarPorCodigo(String codigoBarra);
}
