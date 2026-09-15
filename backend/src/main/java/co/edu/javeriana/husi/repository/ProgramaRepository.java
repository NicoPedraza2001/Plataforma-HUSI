package co.edu.javeriana.husi.repository;
import co.edu.javeriana.husi.model.entity.Programa;
import org.springframework.data.jpa.repository.*;
import java.util.*;
import jakarta.persistence.LockModeType;
public interface ProgramaRepository extends JpaRepository<Programa,Integer> {
 @EntityGraph(attributePaths="universidad")
 List<Programa> findByEstadoOrderByNombreAscIdAsc(String estado);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select p from Programa p where p.id = :id")
 Optional<Programa> bloquearPorId(Integer id);
 @Query(value="select count(*) from residente where id_programa=:id",nativeQuery=true)
 long contarResidentes(Integer id);
 Optional<Programa> findFirstByUniversidadIdAndNombreIgnoreCase(Integer universidadId,String nombre);
}
