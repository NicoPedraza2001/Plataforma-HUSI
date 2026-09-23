package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.Servicio;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface ServicioRepository extends JpaRepository<Servicio, Integer> {
  boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Servicio s where s.id=:id")
  Optional<Servicio> bloquearPorId(Integer id);

  @Query(
    value = "select (select count(*) from asignatura where id_servicio=:id)+(select count(*) from coordinador where id_servicio=:id)",
    nativeQuery = true
  )
  long contarReferencias(Integer id);
}
