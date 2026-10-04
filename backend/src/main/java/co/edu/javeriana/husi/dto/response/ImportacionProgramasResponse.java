package co.edu.javeriana.husi.dto.response;

import java.util.List;

public record ImportacionProgramasResponse(
  int total,
  long validos,
  long creados,
  long omitidos,
  long errores,
  List<Fila> filas
) {
  public record Fila(
    Integer fila,
    String nombre,
    String universidad,
    String estado,
    String mensaje,
    Integer programaId
  ) {}

  public static ImportacionProgramasResponse de(List<Fila> filas) {
    return new ImportacionProgramasResponse(
      filas.size(),
      contar(filas, "VALIDO"),
      contar(filas, "CREADO"),
      contar(filas, "OMITIDO"),
      contar(filas, "ERROR"),
      List.copyOf(filas)
    );
  }

  private static long contar(List<Fila> filas, String estado) {
    return filas
      .stream()
      .filter(fila -> estado.equals(fila.estado()))
      .count();
  }
}
