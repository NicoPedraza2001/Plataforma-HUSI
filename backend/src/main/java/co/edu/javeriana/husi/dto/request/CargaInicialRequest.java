package co.edu.javeriana.husi.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;

public record CargaInicialRequest(
  @NotEmpty @Size(max = 5000) List<Fila> filas,
  boolean crearFaltantes,
  String validacion
) {
  public record Fila(
    Integer fila,
    String universidad,
    String pais,
    String programa,
    String residente,
    String tipoDocumento,
    String documento,
    String perfilAcademico,
    String servicio,
    String cupoMaximo,
    String cupoSimultaneo,
    String asignatura,
    String inicio,
    String fin
  ) {}
}
