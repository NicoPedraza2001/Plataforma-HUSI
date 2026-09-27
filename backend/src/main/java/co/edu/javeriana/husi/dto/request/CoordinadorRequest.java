package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;

public record CoordinadorRequest(
  @NotNull @Positive Integer usuarioId,
  @NotNull @Positive Integer servicioId
) {}
