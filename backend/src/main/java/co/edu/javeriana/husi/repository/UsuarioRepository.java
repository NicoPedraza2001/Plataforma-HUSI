package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.Usuario;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
  boolean existsByCorreoIgnoreCaseAndIdNot(String valor, Integer id);

  Optional<Usuario> findByCorreoIgnoreCase(String correo);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Usuario e where e.id=:id")
  Optional<Usuario> bloquearPorId(Integer id);
}
