package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Cambio de nombres y/o apellidos del usuario en sesión. Un campo en {@code null} se conserva,
 * así la pantalla de ajustes envía solo lo que el usuario modificó.
 */
public record ActualizacionPerfilRequest(
  @Size(max = 100) String nombres,
  @Size(max = 100) String apellidos
) {}
