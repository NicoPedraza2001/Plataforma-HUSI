package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.Coordinador;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface CoordinadorRepository
  extends JpaRepository<Coordinador, Integer>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Coordinador e where e.id=:id")
  Optional<Coordinador> bloquearPorId(Integer id);

  boolean existsByUsuarioIdAndIdNot(Integer usuarioId, Integer id);
}
