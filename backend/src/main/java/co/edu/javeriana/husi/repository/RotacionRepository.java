package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface RotacionRepository
  extends JpaRepository<Rotacion, RotacionId>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Rotacion r where r.id=:id")
  Optional<Rotacion> bloquearPorId(RotacionId id);
}
