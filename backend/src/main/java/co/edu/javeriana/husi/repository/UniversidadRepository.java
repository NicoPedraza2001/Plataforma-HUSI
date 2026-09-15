package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.Universidad;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface UniversidadRepository
  extends JpaRepository<Universidad, Integer>
{
  @Query(
    value = "select count(*) from programa where id_universidad=:id",
    nativeQuery = true
  )
  long contarProgramas(Integer id);

  boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
  List<Universidad> findByEstadoOrderByNombreAsc(String estado);
  List<Universidad> findByNombreIgnoreCaseAndEstado(
    String nombre,
    String estado
  );

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from Universidad u where u.id = :id")
  Optional<Universidad> bloquearPorId(Integer id);
}
