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

  /** Asignaturas no eliminadas con su servicio, para los filtros. */
  @Query(
    "select a from Asignatura a join fetch a.servicio s where a.estado<>'Eliminado' and s.estado<>'Eliminado' order by lower(a.nombre),lower(s.nombre),a.id"
  )
  java.util.List<Asignatura> catalogo();

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
