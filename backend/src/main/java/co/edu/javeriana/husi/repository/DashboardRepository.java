package co.edu.javeriana.husi.repository;

import static co.edu.javeriana.husi.repository.query.DashboardSql.*;

import co.edu.javeriana.husi.dto.request.DashboardFiltros;
import co.edu.javeriana.husi.dto.response.DashboardResponse.ResidenteRevisar;
import co.edu.javeriana.husi.dto.response.DashboardResponse.SituacionPrograma;
import co.edu.javeriana.husi.dto.response.RelacionFiltrosResponse;
import co.edu.javeriana.husi.repository.support.ConsultaSql;
import co.edu.javeriana.husi.repository.support.ParametrosSql;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Lecturas del dashboard. Solo consulta y convierte cada fila a un tipo; los cálculos y textos
 * del panel están en el servicio. Una rotación pertenece al periodo en que inicia, igual que en
 * el resto de la plataforma.
 */
@Repository
@RequiredArgsConstructor
public class DashboardRepository {

  public record Totales(long rotaciones, long asignaciones, long total) {}

  public record Conteo(String etiqueta, long valor) {}

  public record Rango(LocalDate desde, LocalDate hasta) {}

  /** Parte de una asignación que cae dentro de la ventana del mapa de ocupación. */
  public record Tramo(
    String servicio,
    int cupo,
    LocalDate desde,
    LocalDate hasta
  ) {}

  /** Dos rotaciones asignadas del mismo residente y los días que comparten. */
  public record ParCruzado(
    Integer residenteId,
    String residente,
    String programa,
    String asignatura1,
    String servicio1,
    String asignatura2,
    String servicio2,
    LocalDate desde,
    LocalDate hasta
  ) {}

  private final ConsultaSql consultaSql;

  public Totales totales(DashboardFiltros filtros) {
    var parametros = new ParametrosSql();
    var fila = consultar(
      TOTALES.formatted(deRotaciones(filtros, parametros, true)),
      parametros
    ).getFirst();
    return new Totales(
      numero(fila, "rotaciones"),
      numero(fila, "asignaciones"),
      numero(fila, "total")
    );
  }

  public List<Conteo> rotacionesPorServicio(DashboardFiltros filtros) {
    return conteos(ROTACIONES_POR_SERVICIO, filtros, true);
  }

  /** Ignora el filtro de universidad: el reparto siempre compara todas. */
  public List<Conteo> residentesPorUniversidad(DashboardFiltros filtros) {
    return conteos(RESIDENTES_POR_UNIVERSIDAD, filtros, false);
  }

  public List<Conteo> iniciosPorMes(DashboardFiltros filtros) {
    return conteos(INICIOS_POR_MES, filtros, true);
  }

  /**
   * Solo cuenta residentes del periodo: los que tienen alguna rotación no cancelada que inicia
   * en él. En el panel del administrador no responde al servicio ni a la asignatura, porque mide
   * al residente; en el del coordinador sí se limita a su servicio.
   */
  public List<SituacionPrograma> situacionPorPrograma(
    DashboardFiltros filtros
  ) {
    var parametros = new ParametrosSql();
    String sql = SITUACION_POR_PROGRAMA.formatted(
      periodo("rr.fecha_inicio", filtros, parametros) +
        soloDelServicio("rr.id_asignatura", filtros, parametros),
      delResidente(filtros, parametros)
    );
    return consultar(sql, parametros)
      .stream()
      .map(fila ->
        new SituacionPrograma(
          texto(fila, "etiqueta"),
          numero(fila, "conRotacion"),
          numero(fila, "enEspera")
        )
      )
      .toList();
  }

  /** Número de residentes del periodo según cuántas rotaciones tienen en el corte. */
  public Map<Integer, Long> residentesPorCantidadDeRotaciones(
    DashboardFiltros filtros
  ) {
    var parametros = new ParametrosSql();
    String sql = CARGA_POR_RESIDENTE.formatted(
      periodo("rr.fecha_inicio", filtros, parametros) +
        deLaRotacion("s.nombre", "rr.id_asignatura", filtros, parametros),
      periodo("x.fecha_inicio", filtros, parametros) +
        soloDelServicio("x.id_asignatura", filtros, parametros),
      delResidente(filtros, parametros)
    );
    Map<Integer, Long> residentes = new HashMap<>();
    consultar(sql, parametros).forEach(fila ->
      residentes.put(
        (int) numero(fila, "rotaciones"),
        numero(fila, "residentes")
      )
    );
    return residentes;
  }

  /** Primer inicio y último fin entre las asignaciones; vacío si no hay ninguna. */
  public Optional<Rango> rangoAsignaciones() {
    var fila = consultar(RANGO_ASIGNACIONES, new ParametrosSql()).getFirst();
    return fila.get("hasta") == null
      ? Optional.empty()
      : Optional.of(new Rango(fecha(fila, "desde"), fecha(fila, "hasta")));
  }

  /**
   * Tramos de asignación entre {@code desde} y {@code hasta}. Con una asignatura se toma el
   * servicio al que pertenece: el cupo es del servicio, no de la asignatura.
   */
  public List<Tramo> tramosOcupacion(
    DashboardFiltros filtros,
    LocalDate desde,
    LocalDate hasta
  ) {
    var parametros = new ParametrosSql()
      .con("desde", desde)
      .con("hasta", hasta);
    String sql = TRAMOS_OCUPACION.formatted(
      parametros.y("s.nombre = :servicio", "servicio", filtros.servicio()) +
        parametros.y(
          "s.id_servicio = (select id_servicio from asignatura where id_asignatura = :asignaturaId)",
          "asignaturaId",
          filtros.asignaturaId()
        )
    );
    return consultar(sql, parametros)
      .stream()
      .map(fila ->
        new Tramo(
          texto(fila, "servicio"),
          (int) numero(fila, "cupo"),
          fecha(fila, "desde"),
          fecha(fila, "hasta")
        )
      )
      .toList();
  }

  /**
   * Pares de rotaciones que se cruzan. El periodo es el del primer día del cruce, y basta con
   * que una de las dos rotaciones sea del servicio o la asignatura elegidos.
   */
  public List<ParCruzado> paresCruzados(DashboardFiltros filtros) {
    var parametros = new ParametrosSql();
    String sql = PARES_CRUZADOS.formatted(
      periodo("greatest(a.fecha_inicio, b.fecha_inicio)", filtros, parametros) +
        delResidente(filtros, parametros) +
        parametros.y(
          "(sa.nombre = :servicio or sb.nombre = :servicio)",
          "servicio",
          filtros.servicio()
        ) +
        parametros.y(
          "(a.id_asignatura = :asignaturaId or b.id_asignatura = :asignaturaId)",
          "asignaturaId",
          filtros.asignaturaId()
        )
    );
    return consultar(sql, parametros)
      .stream()
      .map(fila ->
        new ParCruzado(
          (int) numero(fila, "residenteId"),
          texto(fila, "residente"),
          texto(fila, "programa"),
          texto(fila, "asignatura1"),
          texto(fila, "servicio1"),
          texto(fila, "asignatura2"),
          texto(fila, "servicio2"),
          fecha(fila, "desde"),
          fecha(fila, "hasta")
        )
      )
      .toList();
  }

  public List<ResidenteRevisar> residentesEnEspera(DashboardFiltros filtros) {
    var parametros = new ParametrosSql();
    String sql = RESIDENTES_EN_ESPERA.formatted(
      periodo("rr.fecha_inicio", filtros, parametros) +
        delResidente(filtros, parametros) +
        deLaRotacion("s.nombre", "rr.id_asignatura", filtros, parametros)
    );
    return consultar(sql, parametros)
      .stream()
      .map(fila ->
        new ResidenteRevisar(
          (int) numero(fila, "residenteId"),
          texto(fila, "residente"),
          texto(fila, "programa"),
          texto(fila, "motivo"),
          texto(fila, "detalle")
        )
      )
      .toList();
  }

  public long serviciosSinCoordinador() {
    return (
      (Number) consultaSql.escalar(SERVICIOS_SIN_COORDINADOR, Map.of())
    ).longValue();
  }

  public List<RelacionFiltrosResponse> relaciones() {
    return consultar(RELACIONES_FILTROS, new ParametrosSql())
      .stream()
      .map(fila ->
        new RelacionFiltrosResponse(
          texto(fila, "programa"),
          texto(fila, "servicio"),
          (int) numero(fila, "asignaturaId")
        )
      )
      .toList();
  }

  // ---------- Condiciones compartidas ----------

  /** Desde la proyección de rotaciones, con los filtros del corte. */
  private String deRotaciones(
    DashboardFiltros filtros,
    ParametrosSql parametros,
    boolean conUniversidad
  ) {
    return DESDE_ROTACIONES.formatted(
      periodo("inicio", filtros, parametros) +
        parametros.y("servicio = :servicio", "servicio", filtros.servicio()) +
        parametros.y("programa = :programa", "programa", filtros.programa()) +
        parametros.y(
          "\"asignaturaId\" = :asignaturaId",
          "asignaturaId",
          filtros.asignaturaId()
        ) +
        (conUniversidad
          ? parametros.y(
              "universidad = :universidad",
              "universidad",
              filtros.universidad()
            )
          : "")
    );
  }

  private String periodo(
    String columna,
    DashboardFiltros filtros,
    ParametrosSql parametros
  ) {
    return parametros.y(
      periodoDe(columna) + " = :periodo",
      "periodo",
      filtros.periodo()
    );
  }

  /** Universidad y programa del residente; las consultas los unen como «u» y «p». */
  private String delResidente(
    DashboardFiltros filtros,
    ParametrosSql parametros
  ) {
    return (
      parametros.y(
        "u.nombre_universidad = :universidad",
        "universidad",
        filtros.universidad()
      ) +
      parametros.y(
        "p.nombre_programa = :programa",
        "programa",
        filtros.programa()
      )
    );
  }

  /** Alcance del coordinador: deja solo las asignaciones en asignaturas de su servicio. */
  private String soloDelServicio(
    String columnaAsignatura,
    DashboardFiltros filtros,
    ParametrosSql parametros
  ) {
    if (!filtros.soloResidentesDelServicio()) return "";
    return parametros.y(
      columnaAsignatura +
        " in (select a2.id_asignatura from asignatura a2 join servicio s2 on s2.id_servicio = a2.id_servicio where s2.nombre = :servicio)",
      "servicio",
      filtros.servicio()
    );
  }

  /** Servicio y asignatura de la rotación, con las columnas de cada consulta. */
  private String deLaRotacion(
    String columnaServicio,
    String columnaAsignatura,
    DashboardFiltros filtros,
    ParametrosSql parametros
  ) {
    return (
      parametros.y(
        columnaServicio + " = :servicio",
        "servicio",
        filtros.servicio()
      ) +
      parametros.y(
        columnaAsignatura + " = :asignaturaId",
        "asignaturaId",
        filtros.asignaturaId()
      )
    );
  }

  // ---------- Lectura de filas ----------

  private List<Conteo> conteos(
    String plantilla,
    DashboardFiltros filtros,
    boolean conUniversidad
  ) {
    var parametros = new ParametrosSql();
    String sql = plantilla.formatted(
      deRotaciones(filtros, parametros, conUniversidad)
    );
    return consultar(sql, parametros)
      .stream()
      .map(fila -> new Conteo(texto(fila, "etiqueta"), numero(fila, "valor")))
      .toList();
  }

  private List<Map<String, Object>> consultar(
    String sql,
    ParametrosSql parametros
  ) {
    return consultaSql.consultar(sql, parametros.valores());
  }

  private static String texto(Map<String, Object> fila, String columna) {
    Object valor = fila.get(columna);
    return valor == null ? null : valor.toString();
  }

  /** Los conteos llegan como Long, BigInteger o Integer según el motor. */
  private static long numero(Map<String, Object> fila, String columna) {
    return ((Number) fila.get(columna)).longValue();
  }

  /** Las fechas llegan como texto o como LocalDate según la consulta y el motor. */
  private static LocalDate fecha(Map<String, Object> fila, String columna) {
    return LocalDate.parse(String.valueOf(fila.get(columna)));
  }
}
