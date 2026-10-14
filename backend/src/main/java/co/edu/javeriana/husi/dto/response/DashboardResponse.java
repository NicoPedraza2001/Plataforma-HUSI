package co.edu.javeriana.husi.dto.response;

import java.util.List;

/** Panel del dashboard para un corte; los nombres son los que consume el frontend. */
public record DashboardResponse(
  List<Indicador> indicadores,
  List<SituacionPrograma> situacionPorPrograma,
  List<Valor> cargaResidentes,
  MapaOcupacion mapaOcupacion,
  List<Barra> rotacionesPorServicio,
  List<Porcentaje> distribucionUniversidad,
  List<Valor> tendenciaInicios,
  List<Alerta> alertas,
  List<ResidenteRevisar> revisar,
  long totalRegistros
) {
  /** Cifra destacada; {@code tono} es la clase de color en la tarjeta. */
  public record Indicador(
    String id,
    String etiqueta,
    String valor,
    String detalle,
    String tono
  ) {}

  /** Residentes del periodo de un programa: con rotación asignada o solo en lista de espera. */
  public record SituacionPrograma(
    String etiqueta,
    long conRotacion,
    long enEspera
  ) {}

  public record Valor(String etiqueta, long valor) {}

  /** Barra horizontal; {@code maximo} fija la escala común de todas las barras. */
  public record Barra(String etiqueta, long valor, long maximo) {}

  public record Porcentaje(String etiqueta, double porcentaje) {}

  public record MapaOcupacion(List<String> meses, List<FilaOcupacion> filas) {
    public static final MapaOcupacion VACIO = new MapaOcupacion(
      List.of(),
      List.of()
    );
  }

  /** Servicio del mapa: pico de cada mes, días sobre el cupo y su mayor porcentaje. */
  public record FilaOcupacion(
    String servicio,
    int cupo,
    long diasExceso,
    List<Celda> celdas,
    int maximo
  ) {}

  public record Celda(String mes, int pico, int porcentaje) {}

  /** {@code nivel}: critico, alerta o info. */
  public record Alerta(
    String id,
    String nivel,
    String etiqueta,
    String detalle,
    long cantidad
  ) {}

  /** {@code motivo}: cruce o espera. En los cruces, {@code detalle} trae uno por línea. */
  public record ResidenteRevisar(
    Integer residenteId,
    String residente,
    String programa,
    String motivo,
    String detalle
  ) {}
}
