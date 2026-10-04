package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.Servicio;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface ServicioRepository extends JpaRepository<Servicio, Integer> {
  @Query(
    "select s from Servicio s where s.estado<>'Eliminado' and (:soloActivos=false or s.estado='Activo') order by lower(s.nombre),s.id"
  )
  java.util.List<Servicio> catalogo(boolean soloActivos);

  boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Servicio s where s.id=:id")
  Optional<Servicio> bloquearPorId(Integer id);

  @Query(
    value = "select (select count(*) from asignatura where id_servicio=:id)+(select count(*) from coordinador where id_servicio=:id)",
    nativeQuery = true
  )
  long contarReferencias(Integer id);

  /** Mayor número de residentes Asignado en una misma rotación vigente del servicio. */
  @Query(
    value = "select coalesce(max(n),0) from (select count(*) n from rotacion_residente rr join asignatura a on a.id_asignatura=rr.id_asignatura where a.id_servicio=:id and rr.estado='Asignado' and rr.fecha_fin>=:hoy group by rr.id_asignatura,rr.fecha_inicio,rr.fecha_fin) t",
    nativeQuery = true
  )
  long maximoAsignadosPorRotacion(Integer id, java.time.LocalDate hoy);
}
