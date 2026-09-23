package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;

public record ResidenteRequest(
  @NotBlank @Size(max = 200) String nombre,
  @NotBlank @Size(max = 30) String tipoDocumento,
  @NotBlank @Size(max = 50) String numeroDocumento,
  @Size(max = 100) String perfilAcademico,
  @NotNull @Positive Integer programaId
) {}
