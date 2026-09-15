package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;

public record EstadoUniversidadRequest(
  @NotNull(message = "Selecciona un estado.")
  @Pattern(
    regexp = "Activo|Inactivo",
    message = "El estado debe ser Activo o Inactivo."
  )
  String estado
) {}
