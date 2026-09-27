package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.Asignatura;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface AsignaturaRepository
  extends JpaRepository<Asignatura, Integer>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Asignatura e where e.id=:id")
  Optional<Asignatura> bloquearPorId(Integer id);

  boolean existsByServicioIdAndNombreIgnoreCaseAndIdNot(
    Integer servicioId,
    String nombre,
    Integer id
  );

  @Query(
    value = "select count(*) from rotacion where id_asignatura=:id",
    nativeQuery = true
  )
  long contarReferencias(Integer id);
}
