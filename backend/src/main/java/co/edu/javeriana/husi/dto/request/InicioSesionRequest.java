package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;

public record InicioSesionRequest(
  @NotBlank @Email @Size(max = 150) String correo,
  @NotBlank @Size(max = 72) String contrasena
) {
  /** Evita que la contraseña aparezca si el objeto llega a imprimirse en un log. */
  @Override
  public String toString() {
    return "InicioSesionRequest[correo=" + correo + ", contrasena=****]";
  }
}
