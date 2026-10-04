package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.UniversidadResponse;
import co.edu.javeriana.husi.model.entity.Universidad;
import org.springframework.stereotype.Component;

@Component
public class UniversidadMapper {

  public UniversidadResponse toResponse(Universidad universidad) {
    return new UniversidadResponse(
      universidad.getId(),
      universidad.getNombre(),
      universidad.getPais(),
      universidad.getPerfil(),
      universidad.getEstado()
    );
  }
}
