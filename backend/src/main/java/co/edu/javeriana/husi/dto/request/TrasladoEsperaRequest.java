package co.edu.javeriana.husi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** Residentes en espera que pasan, ya asignados, a la rotación de destino. */
public record TrasladoEsperaRequest(
  @NotEmpty @Size(max = 200) List<@Valid @NotNull Origen> residentes
) {
  /** Rotación en la que el residente está esperando hoy. */
  public record Origen(
    @NotNull @Positive Integer residenteId,
    @NotNull @Positive Integer asignaturaId,
    @NotNull LocalDate inicio,
    @NotNull LocalDate fin
  ) {}
}
