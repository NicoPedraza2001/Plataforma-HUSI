package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.ServicioResponse;
import co.edu.javeriana.husi.model.entity.Servicio;
import org.springframework.stereotype.Component;

@Component
public class ServicioMapper {

  public ServicioResponse response(Servicio s) {
    return new ServicioResponse(
      s.getId(),
      s.getNombre(),
      s.getDescripcion(),
      s.getCupoSimultaneo(),
      s.getCupoMaximoInstalado(),
      s.getEstado(),
      "Activo".equals(s.getEstado())
    );
  }
}
