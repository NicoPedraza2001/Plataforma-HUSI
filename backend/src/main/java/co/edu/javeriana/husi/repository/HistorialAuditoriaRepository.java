package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.HistorialAuditoria;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface HistorialAuditoriaRepository
  extends JpaRepository<HistorialAuditoria, Integer>
{
  /** Fila resumen de una importación; se bloquea porque cada registro importado la actualiza. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select h from HistorialAuditoria h where h.loteImportacion = :lote")
  Optional<HistorialAuditoria> bloquearPorLote(String lote);
}
