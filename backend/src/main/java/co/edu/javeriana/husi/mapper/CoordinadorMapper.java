package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.CoordinadorResponse;
import co.edu.javeriana.husi.model.entity.Coordinador;
import org.springframework.stereotype.Component;

@Component
public class CoordinadorMapper {

  public CoordinadorResponse response(Coordinador e) {
    return new CoordinadorResponse(
      e.getId(),
      e.getUsuario().getId(),
      e.getUsuario().getNombre() + " " + e.getUsuario().getApellido(),
      e.getUsuario().getCorreo(),
      e.getServicio().getId(),
      e.getServicio().getNombre(),
      e.getEstado(),
      "Activo".equals(e.getEstado())
    );
  }
}
