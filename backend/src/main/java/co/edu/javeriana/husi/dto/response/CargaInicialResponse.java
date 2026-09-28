package co.edu.javeriana.husi.dto.response;

import java.util.*;

public record CargaInicialResponse(
  String validacion,
  boolean guardado,
  int total,
  int validos,
  int advertencias,
  int errores,
  int omitidos,
  Map<String, Integer> nuevos,
  List<Detalle> filas
) {
  public record Detalle(int fila, String estado, String mensaje) {}
}
