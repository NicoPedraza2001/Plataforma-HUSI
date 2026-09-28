package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RotacionRequest(
  @NotNull @Positive Integer asignaturaId,
  @NotNull LocalDate inicio,
  @NotNull LocalDate fin
) {
  @AssertTrue(
    message = "La fecha final debe ser igual o posterior a la inicial, entre 1900 y 9999."
  )
  public boolean isFechasValidas() {
    return (
      inicio == null ||
      fin == null ||
      (!fin.isBefore(inicio) &&
        inicio.getYear() >= 1900 &&
        fin.getYear() <= 9999)
    );
  }
}
