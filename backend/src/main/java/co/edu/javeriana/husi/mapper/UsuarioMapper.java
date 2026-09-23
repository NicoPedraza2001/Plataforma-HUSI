package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.UsuarioResponse;
import co.edu.javeriana.husi.model.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

  public UsuarioResponse response(Usuario e) {
    return new UsuarioResponse(
      e.getId(),
      e.getNombre(),
      e.getApellido(),
      e.getCorreo(),
      e.getRolId(),
      e.getEstado()
    );
  }
}
