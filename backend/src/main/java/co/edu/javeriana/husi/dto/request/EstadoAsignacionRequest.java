package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;

public record EstadoAsignacionRequest(
  @NotBlank @Pattern(regexp = "Asignado|Cancelado|Esperando cupo") String estado
) {}
