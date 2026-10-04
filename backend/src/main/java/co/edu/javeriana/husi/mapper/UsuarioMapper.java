package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.UsuarioResponse;
import co.edu.javeriana.husi.model.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

  public UsuarioResponse toResponse(Usuario usuario) {
    return new UsuarioResponse(
      usuario.getId(),
      usuario.getNombre(),
      usuario.getApellido(),
      usuario.getCorreo(),
      usuario.getRolId(),
      usuario.getEstado()
    );
  }
}
