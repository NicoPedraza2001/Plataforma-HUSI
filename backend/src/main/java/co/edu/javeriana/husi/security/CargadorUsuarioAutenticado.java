package co.edu.javeriana.husi.security;

import co.edu.javeriana.husi.model.entity.Usuario;
import co.edu.javeriana.husi.model.enums.RolUsuario;
import co.edu.javeriana.husi.repository.RolRepository;
import co.edu.javeriana.husi.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Busca en la base de datos el usuario que intenta iniciar sesión. Spring Security lo usa para
 * comparar la contraseña escrita con el hash guardado en {@code usuario.contrasena}.
 *
 * <p>Un usuario cuyo rol no da acceso a la plataforma se trata igual que uno inexistente.
 */
@Service
@RequiredArgsConstructor
public class CargadorUsuarioAutenticado implements UserDetailsService {

  private final UsuarioRepository usuarioRepository;
  private final RolRepository rolRepository;

  @Override
  public UsuarioAutenticado loadUserByUsername(String correo) {
    Usuario usuario = usuarioRepository
      .findByCorreoIgnoreCase(correo.strip())
      .filter(encontrado -> !"Eliminado".equals(encontrado.getEstado()))
      .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));
    RolUsuario rol = RolUsuario.desdeNombreEnBaseDeDatos(
      rolRepository.consultarNombre(usuario.getRolId())
    ).orElseThrow(() -> new UsernameNotFoundException("El rol no tiene acceso."));
    return new UsuarioAutenticado(
      usuario.getId(),
      usuario.getCorreo(),
      usuario.getContrasena(),
      rol,
      "Activo".equals(usuario.getEstado())
    );
  }
}
