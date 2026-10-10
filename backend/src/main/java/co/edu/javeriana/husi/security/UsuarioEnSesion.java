package co.edu.javeriana.husi.security;

import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.model.entity.Usuario;
import co.edu.javeriana.husi.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Usuario responsable de los cambios de la petición en curso, el que firma cada fila del
 * historial de auditoría.
 *
 * <p>La plataforma aún no tiene inicio de sesión: mientras tanto se usa el usuario cuyo correo
 * indica {@code husi.auditoria.correo-usuario-sin-sesion}. Al integrar Spring Security, este es
 * el único punto que debe cambiar para leer el usuario autenticado.
 */
@Component
@RequiredArgsConstructor
public class UsuarioEnSesion {

  private final UsuarioRepository usuarioRepository;

  @Value("${husi.auditoria.correo-usuario-sin-sesion}")
  private String correoUsuarioSinSesion;

  public Usuario consultar() {
    return usuarioRepository
      .findByCorreoIgnoreCase(correoUsuarioSinSesion)
      .orElseThrow(() ->
        new NegocioException(
          HttpStatus.CONFLICT,
          "No se encontró el usuario " +
            correoUsuarioSinSesion +
            " que firma el historial de cambios. Créalo o ajusta husi.auditoria.correo-usuario-sin-sesion."
        )
      );
  }
}
