package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.repository.query.ListadoSql;
import co.edu.javeriana.husi.repository.support.ConsultaSql;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class DashboardRepository {

  private final ConsultaSql repository;

  public Map<String, Object> consultar(Map<String, String> filtros) {
    Map<String, Object> params = new HashMap<>();
    String where = " where estado<>'Cancelado'";

    if (filtros.containsKey("periodo")) {
      where +=
        " and extract(year from inicio)::int::text || '-' || case when extract(month from inicio)<=6 then '1' else '2' end = :periodo";
      params.put("periodo", filtros.get("periodo"));
    }
    if (filtros.containsKey("servicio")) {
      where += " and servicio=:servicio";
      params.put("servicio", filtros.get("servicio"));
    }
    String repartoWhere = where;
    Map<String, Object> repartoParams = new HashMap<>(params);
    if (filtros.containsKey("universidad")) {
      where += " and universidad=:universidad";
      params.put("universidad", filtros.get("universidad"));
    }
    String base = " from (" + ListadoSql.ROTACIONES + ") datos";
    var counts = repository
      .consultar(
        "select count(distinct (\"asignaturaId\",inicio,fin)) filter(where inicio<=(current_timestamp at time zone 'America/Bogota')::date and fin>=(current_timestamp at time zone 'America/Bogota')::date) as activas,count(distinct (\"asignaturaId\",inicio,fin)) filter(where inicio>(current_timestamp at time zone 'America/Bogota')::date and inicio<=(current_timestamp at time zone 'America/Bogota')::date+30) as proximas,count(distinct \"residenteId\") filter(where estado='Asignado' and inicio<=(current_timestamp at time zone 'America/Bogota')::date and fin>=(current_timestamp at time zone 'America/Bogota')::date) as residentes,count(*) as total" +
          base +
          where,
        params
      )
      .getFirst();
    var porServicio = repository.consultar(
      "select servicio as etiqueta,count(distinct (\"asignaturaId\",inicio,fin)) as valor" +
        base +
        where +
        " group by servicio order by servicio",
      params
    );
    long max = porServicio
      .stream()
      .mapToLong(row -> ((Number) row.get("valor")).longValue())
      .max()
      .orElse(1);
    porServicio.forEach(row -> row.put("maximo", max));
    var reparto = repository.consultar(
      "select universidad as etiqueta,count(distinct \"residenteId\") as valor" +
        base +
        repartoWhere +
        " group by universidad order by valor desc,universidad",
      repartoParams
    );
    double suma = reparto
      .stream()
      .mapToDouble(row -> ((Number) row.get("valor")).doubleValue())
      .sum();
    List<Map<String, Object>> distribucion = new ArrayList<>();
    double otras = 0;
    for (int i = 0; i < reparto.size(); i++) {
      var row = reparto.get(i);
      double porcentaje =
        suma == 0
          ? 0
          : (((Number) row.get("valor")).doubleValue() * 100) / suma;
      if (i < 3 || reparto.size() <= 4) distribucion.add(
        Map.of(
          "etiqueta",
          row.get("etiqueta"),
          "porcentaje",
          Math.round(porcentaje * 10) / 10.0
        )
      );
      else otras += porcentaje;
    }
    if (otras > 0) distribucion.add(
      Map.of("etiqueta", "Otras", "porcentaje", Math.round(otras * 10) / 10.0)
    );
    var tendencia = repository.consultar(
      "select to_char(inicio,'YYYY-MM') as etiqueta,count(distinct (\"asignaturaId\",inicio,fin)) as valor" +
        base +
        where +
        " group by to_char(inicio,'YYYY-MM') order by etiqueta",
      params
    );
    // Capacidad y ocupación actuales del servicio; no se divide la capacidad por universidad.
    Map<String, Object> ocupParams = new HashMap<>();
    String filtroServicio = "";
    if (filtros.containsKey("servicio")) {
      filtroServicio = " and s.nombre=:servicio";
      ocupParams.put("servicio", filtros.get("servicio"));
    }
    var ocupacion = repository.consultar(
      "select s.nombre as etiqueta,count(distinct rr.id_residente) as valor,s.cupo_simultaneo as maximo from servicio s left join asignatura a using(id_servicio) left join rotacion_residente rr on rr.id_asignatura=a.id_asignatura and rr.estado='Asignado' and rr.fecha_inicio<=(current_timestamp at time zone 'America/Bogota')::date and rr.fecha_fin>=(current_timestamp at time zone 'America/Bogota')::date where s.estado='Activo'" +
        filtroServicio +
        " group by s.id_servicio,s.nombre,s.cupo_simultaneo order by s.nombre",
      ocupParams
    );
    double usados = ocupacion
      .stream()
      .mapToDouble(row -> ((Number) row.get("valor")).doubleValue())
      .sum();
    double cupos = ocupacion
      .stream()
      .mapToDouble(row -> ((Number) row.get("maximo")).doubleValue())
      .sum();
    List<Map<String, Object>> indicadores = List.of(
      indicador(
        "activas",
        "Rotaciones activas",
        counts.get("activas"),
        "Hoy, dentro del corte seleccionado",
        "teal"
      ),
      indicador(
        "proximas",
        "Próximas rotaciones",
        counts.get("proximas"),
        "Inician en los siguientes 30 días",
        "naranja"
      ),
      indicador(
        "residentes",
        "Residentes con rotación",
        counts.get("residentes"),
        "Asignados hoy, dentro del corte",
        "azul"
      ),
      indicador(
        "ocupacion",
        "Ocupación actual",
        cupos == 0 ? "Sin capacidad" : Math.round((usados * 100) / cupos) + "%",
        "Servicios activos; todas las universidades",
        "verde"
      )
    );
    long espera = (
      (Number) repository.escalar(
        "select count(*) from rotacion_residente where estado='Esperando cupo' and fecha_fin>=(current_timestamp at time zone 'America/Bogota')::date",
        Map.of()
      )
    ).longValue();
    List<Map<String, Object>> alertas = new ArrayList<>();
    if (espera > 0) alertas.add(
      Map.of(
        "id",
        "espera",
        "nivel",
        "alerta",
        "etiqueta",
        "Asignaciones en espera",
        "detalle",
        "Rotaciones vigentes o futuras",
        "cantidad",
        espera
      )
    );
    return Map.of(
      "indicadores",
      indicadores,
      "rotacionesPorServicio",
      porServicio,
      "distribucionUniversidad",
      distribucion,
      "tendenciaInicios",
      tendencia,
      "ocupacionPorServicio",
      ocupacion,
      "alertas",
      alertas,
      "totalRegistros",
      counts.get("total")
    );
  }

  private Map<String, Object> indicador(
    String id,
    String etiqueta,
    Object valor,
    String detalle,
    String tono
  ) {
    return Map.of(
      "id",
      id,
      "etiqueta",
      etiqueta,
      "valor",
      valor.toString(),
      "detalle",
      detalle,
      "tono",
      tono
    );
  }
}
