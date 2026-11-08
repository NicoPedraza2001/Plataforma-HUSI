package co.edu.javeriana.husi.dto.request;

import java.util.Map;

/**
 * Corte del dashboard. Un filtro en null no se aplica. El periodo tiene el formato AAAA-1
 * (enero a junio) o AAAA-2 (julio a diciembre). Con {@code soloResidentesDelServicio}, las cifras
 * que miden al residente tampoco salen del servicio: es el alcance del coordinador.
 */
public record DashboardFiltros(
  String periodo,
  String universidad,
  String programa,
  String servicio,
  Integer asignaturaId,
  boolean soloResidentesDelServicio
) {
  /** Desde los parámetros ya validados de la petición; los vacíos cuentan como «sin filtro». */
  public static DashboardFiltros de(Map<String, String> parametros) {
    String asignaturaId = valor(parametros, "asignaturaId");
    return new DashboardFiltros(
      valor(parametros, "periodo"),
      valor(parametros, "universidad"),
      valor(parametros, "programa"),
      valor(parametros, "servicio"),
      asignaturaId == null ? null : Integer.valueOf(asignaturaId),
      false
    );
  }

  /** Corte del coordinador: nada de lo que se consulte sale de su servicio. */
  public static DashboardFiltros soloDelServicio(String servicio) {
    return new DashboardFiltros(null, null, null, servicio, null, true);
  }

  private static String valor(Map<String, String> parametros, String nombre) {
    String valor = parametros.get(nombre);
    return valor == null || valor.isBlank() ? null : valor.strip();
  }
}
