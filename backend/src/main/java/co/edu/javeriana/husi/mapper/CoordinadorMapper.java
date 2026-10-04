package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.CoordinadorResponse;
import co.edu.javeriana.husi.model.entity.Coordinador;
import org.springframework.stereotype.Component;

@Component
public class CoordinadorMapper {

  public CoordinadorResponse toResponse(Coordinador coordinador) {
    return new CoordinadorResponse(
      coordinador.getId(),
      coordinador.getUsuario().getId(),
      coordinador.getUsuario().getNombre() + " " + coordinador.getUsuario().getApellido(),
      coordinador.getUsuario().getCorreo(),
      coordinador.getServicio().getId(),
      coordinador.getServicio().getNombre(),
      coordinador.getEstado(),
      "Activo".equals(coordinador.getEstado())
    );
  }
}
