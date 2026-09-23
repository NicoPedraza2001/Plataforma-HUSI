package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.ResidenteResponse;
import co.edu.javeriana.husi.model.entity.Residente;
import org.springframework.stereotype.Component;

@Component
public class ResidenteMapper {

  public ResidenteResponse response(Residente e) {
    return new ResidenteResponse(
      e.getId(),
      e.getNombre(),
      e.getTipoDocumento(),
      e.getNumeroDocumento(),
      e.getPerfilAcademico(),
      e.getProgramaId(),
      e.getEstado()
    );
  }
}
