package co.edu.javeriana.husi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ImportacionProgramasRequest(
  @NotEmpty(message = "El archivo no contiene registros.")
  @Size(max = 1000, message = "Importa hasta 1000 filas por archivo.")
  List<@NotNull @Valid Fila> filas
) {
  public record Fila(
    @NotNull @Positive Integer fila,
    String nombre,
    String universidad
  ) {}
}
