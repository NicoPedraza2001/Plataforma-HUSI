package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.Coordinador;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

/**
 * Servicio que coordina cada usuario con rol Coordinador. Lo escribe UsuarioServiceImpl al crear o
 * editar el usuario; no tiene pantalla propia.
 */
public interface CoordinadorRepository extends JpaRepository<Coordinador, Integer> {
  Optional<Coordinador> findByUsuarioId(Integer usuarioId);
}
