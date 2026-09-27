package co.edu.javeriana.husi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Resultado tipado; mantiene los nombres JSON que consume el frontend. */
public record ImportacionRotacionesResponse(
  int total,
  @JsonProperty("VALIDO") long validos,
  @JsonProperty("CREADO") long creados,
  @JsonProperty("OMITIDO") long omitidos,
  @JsonProperty("ERROR") long errores,
  List<Fila> filas
) {
  public record Fila(int fila, String estado, String mensaje) {}
}
