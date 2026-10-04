package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.AsignaturaResponse;
import co.edu.javeriana.husi.model.entity.Asignatura;
import org.springframework.stereotype.Component;

@Component
public class AsignaturaMapper {

  public AsignaturaResponse toResponse(Asignatura asignatura) {
    return new AsignaturaResponse(
      asignatura.getId(),
      asignatura.getNombre(),
      asignatura.getServicio().getId(),
      asignatura.getServicio().getNombre(),
      asignatura.getEstado(),
      "Activo".equals(asignatura.getEstado())
    );
  }
}
