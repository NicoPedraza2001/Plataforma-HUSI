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

  private final ConsultaSql consultaSql;

  public Map<String, Object> consultar(Map<String, String> filtros) {
    Map<String, Object> parametros = new HashMap<>();
    String where = " where estado<>'Cancelado'";

    if (filtros.containsKey("periodo")) {
      where +=
        " and extract(year from inicio)::int::text || '-' || case when extract(month from inicio)<=6 then '1' else '2' end = :periodo";
      parametros.put("periodo", filtros.get("periodo"));
    }
    if (filtros.containsKey("servicio")) {
      where += " and servicio=:servicio";
      parametros.put("servicio", filtros.get("servicio"));
    }
    // El reparto por universidad ignora el filtro de universidad: siempre muestra todas.
    String whereReparto = where;
    Map<String, Object> parametrosReparto = new HashMap<>(parametros);
    if (filtros.containsKey("universidad")) {
      where += " and universidad=:universidad";
      parametros.put("universidad", filtros.get("universidad"));
    }
    String base = " from (" + ListadoSql.ROTACIONES + ") datos";
    var totales = consultaSql
      .consultar(
        "select count(distinct (\"asignaturaId\",inicio,fin)) filter(where inicio<=(current_timestamp at time zone 'America/Bogota')::date and fin>=(current_timestamp at time zone 'America/Bogota')::date) as activas,count(distinct (\"asignaturaId\",inicio,fin)) filter(where inicio>(current_timestamp at time zone 'America/Bogota')::date and inicio<=(current_timestamp at time zone 'America/Bogota')::date+30) as proximas,count(distinct \"residenteId\") filter(where estado='Asignado' and inicio<=(current_timestamp at time zone 'America/Bogota')::date and fin>=(current_timestamp at time zone 'America/Bogota')::date) as residentes,count(*) as total" +
          base +
          where,
        parametros
      )
      .getFirst();
    var porServicio = consultaSql.consultar(
      "select servicio as etiqueta,count(distinct (\"asignaturaId\",inicio,fin)) as valor" +
        base +
        where +
        " group by servicio order by servicio",
      parametros
    );
    long maximoPorServicio = porServicio
      .stream()
      .mapToLong(fila -> ((Number) fila.get("valor")).longValue())
      .max()
      .orElse(1);
    porServicio.forEach(fila -> fila.put("maximo", maximoPorServicio));
    var reparto = consultaSql.consultar(
      "select universidad as etiqueta,count(distinct \"residenteId\") as valor" +
        base +
        whereReparto +
        " group by universidad order by valor desc,universidad",
      parametrosReparto
    );
    double totalResidentes = reparto
      .stream()
      .mapToDouble(fila -> ((Number) fila.get("valor")).doubleValue())
      .sum();
    // Se muestran las 3 universidades principales y el resto se agrupa en «Otras».
    List<Map<String, Object>> distribucion = new ArrayList<>();
    double porcentajeOtras = 0;
    for (int i = 0; i < reparto.size(); i++) {
      var fila = reparto.get(i);
      double porcentaje =
        totalResidentes == 0
          ? 0
          : (((Number) fila.get("valor")).doubleValue() * 100) / totalResidentes;
      if (i < 3 || reparto.size() <= 4) distribucion.add(
        Map.of(
          "etiqueta",
          fila.get("etiqueta"),
          "porcentaje",
          Math.round(porcentaje * 10) / 10.0
        )
      );
      else porcentajeOtras += porcentaje;
    }
    if (porcentajeOtras > 0) distribucion.add(
      Map.of(
        "etiqueta",
        "Otras",
        "porcentaje",
        Math.round(porcentajeOtras * 10) / 10.0
      )
    );
    var tendencia = consultaSql.consultar(
      "select to_char(inicio,'YYYY-MM') as etiqueta,count(distinct (\"asignaturaId\",inicio,fin)) as valor" +
        base +
        where +
        " group by to_char(inicio,'YYYY-MM') order by etiqueta",
      parametros
    );
    // Capacidad y ocupación actuales del servicio; no se divide la capacidad por universidad.
    Map<String, Object> parametrosOcupacion = new HashMap<>();
    String filtroServicio = "";
    if (filtros.containsKey("servicio")) {
      filtroServicio = " and s.nombre=:servicio";
      parametrosOcupacion.put("servicio", filtros.get("servicio"));
    }
    var ocupacion = consultaSql.consultar(
      "select s.nombre as etiqueta,count(distinct rr.id_residente) as valor,s.cupo_simultaneo as maximo from servicio s left join asignatura a using(id_servicio) left join rotacion_residente rr on rr.id_asignatura=a.id_asignatura and rr.estado='Asignado' and rr.fecha_inicio<=(current_timestamp at time zone 'America/Bogota')::date and rr.fecha_fin>=(current_timestamp at time zone 'America/Bogota')::date where s.estado='Activo'" +
        filtroServicio +
        " group by s.id_servicio,s.nombre,s.cupo_simultaneo order by s.nombre",
      parametrosOcupacion
    );
    double cuposUsados = ocupacion
      .stream()
      .mapToDouble(fila -> ((Number) fila.get("valor")).doubleValue())
      .sum();
    double cuposTotales = ocupacion
      .stream()
      .mapToDouble(fila -> ((Number) fila.get("maximo")).doubleValue())
      .sum();
    List<Map<String, Object>> indicadores = List.of(
      indicador(
        "activas",
        "Rotaciones activas",
        totales.get("activas"),
        "Hoy, dentro del corte seleccionado",
        "teal"
      ),
      indicador(
        "proximas",
        "Próximas rotaciones",
        totales.get("proximas"),
        "Inician en los siguientes 30 días",
        "naranja"
      ),
      indicador(
        "residentes",
        "Residentes con rotación",
        totales.get("residentes"),
        "Asignados hoy, dentro del corte",
        "azul"
      ),
      indicador(
        "ocupacion",
        "Ocupación actual",
        cuposTotales == 0
          ? "Sin capacidad"
          : Math.round((cuposUsados * 100) / cuposTotales) + "%",
        "Servicios activos; todas las universidades",
        "verde"
      )
    );
    long asignacionesEnEspera = (
      (Number) consultaSql.escalar(
        "select count(*) from rotacion_residente where estado='Esperando cupo' and fecha_fin>=(current_timestamp at time zone 'America/Bogota')::date",
        Map.of()
      )
    ).longValue();
    List<Map<String, Object>> alertas = new ArrayList<>();
    if (asignacionesEnEspera > 0) alertas.add(
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
        asignacionesEnEspera
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
      totales.get("total")
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
