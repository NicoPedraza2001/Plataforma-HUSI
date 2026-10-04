package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.ServicioResponse;
import co.edu.javeriana.husi.model.entity.Servicio;
import org.springframework.stereotype.Component;

@Component
public class ServicioMapper {

  public ServicioResponse toResponse(Servicio servicio) {
    return new ServicioResponse(
      servicio.getId(),
      servicio.getNombre(),
      servicio.getDescripcion(),
      servicio.getCupoSimultaneo(),
      servicio.getCupoMaximoInstalado(),
      servicio.getEstado(),
      "Activo".equals(servicio.getEstado())
    );
  }
}
