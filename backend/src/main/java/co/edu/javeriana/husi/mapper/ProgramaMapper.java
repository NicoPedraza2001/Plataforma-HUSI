package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.ProgramaResponse;
import co.edu.javeriana.husi.model.entity.Programa;
import org.springframework.stereotype.Component;

@Component
public class ProgramaMapper {

  public ProgramaResponse toResponse(Programa programa) {
    return new ProgramaResponse(
      programa.getId(),
      programa.getNombre(),
      programa.getUniversidad().getId(),
      programa.getUniversidad().getNombre(),
      programa.getEstado()
    );
  }
}
