package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;

/**
 * Cambio de correo y/o contraseña del usuario en sesión. Siempre exige la contraseña actual; un
 * campo nuevo en {@code null} se conserva.
 */
public record ActualizacionCredencialesRequest(
  @NotBlank @Size(max = 72) String contrasenaActual,
  @Email @Size(max = 150) String correo,
  @Size(max = 72) String contrasenaNueva
) {
  /** Evita que las contraseñas aparezcan si el objeto llega a imprimirse en un log. */
  @Override
  public String toString() {
    return (
      "ActualizacionCredencialesRequest[correo=" +
      correo +
      ", contrasenaActual=****, contrasenaNueva=****]"
    );
  }
}
