package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.ResidenteResponse;
import co.edu.javeriana.husi.model.entity.Residente;
import org.springframework.stereotype.Component;

@Component
public class ResidenteMapper {

  public ResidenteResponse toResponse(Residente residente) {
    return new ResidenteResponse(
      residente.getId(),
      residente.getNombre(),
      residente.getTipoDocumento().getCodigo(),
      residente.getNumeroDocumento(),
      residente.getPerfilAcademico() == null ? null : residente.getPerfilAcademico().getNombre(),
      residente.getProgramaId(),
      residente.getEstado()
    );
  }
}
