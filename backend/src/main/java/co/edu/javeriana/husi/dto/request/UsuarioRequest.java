package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;

public record UsuarioRequest(
  @NotBlank @Size(max = 100) String nombre,
  @NotBlank @Size(max = 100) String apellido,
  @NotBlank @Email @Size(max = 150) String correo,
  @NotNull @Positive Integer rolId,
  @Size(max = 72) String contrasena
) {}
