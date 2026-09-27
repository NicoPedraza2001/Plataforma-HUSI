package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;

public record AsignaturaRequest(
  @NotBlank @Size(max = 200) String nombre,
  @NotNull @Positive Integer servicioId
) {}
