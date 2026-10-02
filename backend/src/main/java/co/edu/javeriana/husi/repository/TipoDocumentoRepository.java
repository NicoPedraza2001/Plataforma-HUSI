package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.model.entity.TipoDocumento;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoDocumentoRepository
  extends JpaRepository<TipoDocumento, Integer> {
  Optional<TipoDocumento> findByCodigoIgnoreCase(String codigo);

  List<TipoDocumento> findAllByOrderByCodigo();
}
