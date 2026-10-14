package co.edu.javeriana.husi.security;

import co.edu.javeriana.husi.model.enums.RolUsuario;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Usuario que inició sesión, tal como queda guardado en la sesión del servidor.
 *
 * <p>Solo conserva el id, el correo y el rol. El hash de la contraseña se necesita únicamente
 * mientras Spring Security lo compara; después {@link #eraseCredentials()} lo borra para que no
 * quede en memoria dentro de la sesión.
 */
public class UsuarioAutenticado implements UserDetails, CredentialsContainer {

  private final Integer id;
  private final String correo;
  private final RolUsuario rol;
  private final boolean activo;
  private String hashContrasena;

  public UsuarioAutenticado(
    Integer id,
    String correo,
    String hashContrasena,
    RolUsuario rol,
    boolean activo
  ) {
    this.id = id;
    this.correo = correo;
    this.hashContrasena = hashContrasena;
    this.rol = rol;
    this.activo = activo;
  }

  public Integer getId() {
    return id;
  }

  public RolUsuario getRol() {
    return rol;
  }

  @Override
  public String getUsername() {
    return correo;
  }

  @Override
  public String getPassword() {
    return hashContrasena;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
  }

  @Override
  public boolean isEnabled() {
    return activo;
  }

  @Override
  public void eraseCredentials() {
    hashContrasena = null;
  }
}
