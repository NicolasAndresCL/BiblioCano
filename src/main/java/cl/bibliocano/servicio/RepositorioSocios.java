package cl.bibliocano.servicio;

import cl.bibliocano.dominio.Socio;
import java.util.Optional;

/**
 * Acceso a los socios. La define el servicio (lo que necesita) y la implementa la persistencia
 * (memoria o PostgreSQL): así las reglas se prueban sin base de datos.
 */
public interface RepositorioSocios {

    /** Devuelve el socio con sus préstamos activos cargados, para poder validar su estado. */
    Optional<Socio> buscarPorId(int idSocio);
}
