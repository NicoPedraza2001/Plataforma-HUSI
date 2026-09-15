package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.UniversidadResponse;
import co.edu.javeriana.husi.model.entity.Universidad;
import org.springframework.stereotype.Component;

@Component
public class UniversidadMapper {

  public UniversidadResponse response(Universidad u) {
    return new UniversidadResponse(
      u.getId(),
      u.getNombre(),
      u.getPais(),
      u.getPerfil(),
      u.getEstado()
    );
  }
}
