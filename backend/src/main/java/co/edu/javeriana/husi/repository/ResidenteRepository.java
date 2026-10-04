package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.Residente;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface ResidenteRepository extends JpaRepository<Residente, Integer> {
  boolean existsByNumeroDocumentoIgnoreCaseAndIdNot(String valor, Integer id);

  @Query(
    "select e from Residente e join fetch e.tipoDocumento left join fetch e.perfilAcademico " +
      "where lower(e.numeroDocumento) in :documentos and e.estado<>'Eliminado'"
  )
  List<Residente> buscarPorDocumentos(Collection<String> documentos);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Residente e where e.id=:id")
  Optional<Residente> bloquearPorId(Integer id);

  @Query(
    value = "select count(*) from rotacion_residente where id_residente=:id",
    nativeQuery = true
  )
  long contarReferencias(Integer id);
}
