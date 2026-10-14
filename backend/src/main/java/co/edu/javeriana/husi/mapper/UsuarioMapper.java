package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.UsuarioResponse;
import co.edu.javeriana.husi.model.entity.Coordinador;
import co.edu.javeriana.husi.model.entity.Usuario;
import co.edu.javeriana.husi.repository.CoordinadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsuarioMapper {

  private final CoordinadorRepository coordinadorRepository;

  public UsuarioResponse toResponse(Usuario usuario) {
    var servicio = coordinadorRepository
      .findByUsuarioId(usuario.getId())
      .map(Coordinador::getServicio);
    return new UsuarioResponse(
      usuario.getId(),
      usuario.getNombre(),
      usuario.getApellido(),
      usuario.getCorreo(),
      usuario.getRolId(),
      usuario.getEstado(),
      servicio.map(s -> s.getId()).orElse(null),
      servicio.map(s -> s.getNombre()).orElse(null)
    );
  }
}
