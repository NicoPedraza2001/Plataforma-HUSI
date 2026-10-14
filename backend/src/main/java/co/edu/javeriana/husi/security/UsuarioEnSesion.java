package co.edu.javeriana.husi.security;

import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.model.entity.Usuario;
import co.edu.javeriana.husi.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Usuario que hizo la petición en curso: el que firma cada fila del historial de auditoría y el
 * que consulta su perfil. Lo toma de la sesión iniciada en {@code POST /sesion/iniciar}.
 */
@Component
@RequiredArgsConstructor
public class UsuarioEnSesion {

  private final UsuarioRepository usuarioRepository;

  public Usuario consultar() {
    return usuarioRepository
      .findById(idUsuarioAutenticado())
      .filter(usuario -> "Activo".equals(usuario.getEstado()))
      .orElseThrow(() ->
        new NegocioException(
          HttpStatus.UNAUTHORIZED,
          "Tu usuario ya no está activo. Inicia sesión nuevamente."
        )
      );
  }

  private Integer idUsuarioAutenticado() {
    Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
    if (
      autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario
    ) return usuario.getId();
    throw new NegocioException(HttpStatus.UNAUTHORIZED, "No has iniciado sesión.");
  }
}
