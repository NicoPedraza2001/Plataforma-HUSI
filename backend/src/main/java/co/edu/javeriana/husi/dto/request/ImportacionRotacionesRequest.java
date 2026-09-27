package co.edu.javeriana.husi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ImportacionRotacionesRequest(
  @NotNull @Size(min = 1, max = 1000) List<@NotNull @Valid Fila> filas
) {
  public record Fila(
    @NotNull @Positive Integer fila,
    @Size(max = 200) String asignatura,
    @Size(max = 150) String servicio,
    @Size(max = 50) String inicio,
    @Size(max = 50) String fin
  ) {}
}
