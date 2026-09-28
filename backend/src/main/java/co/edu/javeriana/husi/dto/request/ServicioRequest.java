package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;

public record ServicioRequest(
  @NotBlank @Size(max = 150) String nombre,
  String descripcion,
  @NotNull @Min(0) Integer cupoSimultaneo,
  @NotNull @Min(0) Integer cupoMaximoInstalado
) {
  @AssertTrue(
    message = "El cupo simultáneo no puede superar el máximo instalado."
  )
  public boolean isCuposCoherentes() {
    return (
      cupoSimultaneo == null ||
      cupoMaximoInstalado == null ||
      cupoSimultaneo <= cupoMaximoInstalado
    );
  }
}
