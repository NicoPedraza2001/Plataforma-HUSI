package co.edu.javeriana.husi.security;

import co.edu.javeriana.husi.model.enums.RolUsuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Usuario que inició sesión, tal como queda guardado en la sesión del servidor.
 *
 * <p>Solo conserva el id, el correo, el rol y una huella de las credenciales. El hash de la
 * contraseña se necesita únicamente mientras Spring Security lo compara; después
 * {@link #eraseCredentials()} lo borra para que no quede en memoria dentro de la sesión.
 */
public class UsuarioAutenticado implements UserDetails, CredentialsContainer {

  private final Integer id;
  private final String correo;
  private final RolUsuario rol;
  private final boolean activo;
  /** Permite saber si el correo o la contraseña cambiaron sin guardar el hash en la sesión. */
  private final String huellaCredenciales;
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
    this.huellaCredenciales = calcularHuellaCredenciales(correo, hashContrasena);
  }

  private static String calcularHuellaCredenciales(String correo, String hashContrasena) {
    try {
      byte[] huella = MessageDigest.getInstance("SHA-256").digest(
        (correo + ":" + hashContrasena).getBytes(StandardCharsets.UTF_8)
      );
      return HexFormat.of().formatHex(huella);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("La JVM no ofrece SHA-256.", e);
    }
  }

  /** ¿La cuenta sigue teniendo el acceso con el que se abrió esta sesión? */
  public boolean conservaElAccesoDe(UsuarioAutenticado sesionAbierta) {
    return (
      id.equals(sesionAbierta.id) &&
      rol == sesionAbierta.rol &&
      activo &&
      huellaCredenciales.equals(sesionAbierta.huellaCredenciales)
    );
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
