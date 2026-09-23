package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;

public record EstadoRequest(
  @NotNull @Pattern(regexp = "Activo|Inactivo") String estado
) {}
