package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.AsignaturaResponse;
import co.edu.javeriana.husi.model.entity.Asignatura;
import org.springframework.stereotype.Component;

@Component
public class AsignaturaMapper {

  public AsignaturaResponse response(Asignatura e) {
    return new AsignaturaResponse(
      e.getId(),
      e.getNombre(),
      e.getServicio().getId(),
      e.getServicio().getNombre(),
      e.getEstado(),
      "Activo".equals(e.getEstado())
    );
  }
}
