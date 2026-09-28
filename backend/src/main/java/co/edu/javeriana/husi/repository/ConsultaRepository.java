package co.edu.javeriana.husi.repository;

import static co.edu.javeriana.husi.repository.query.ListadoSql.*;

import co.edu.javeriana.husi.dto.response.PaginaResponse;
import co.edu.javeriana.husi.repository.support.ConsultaSql;
import jakarta.persistence.EntityManager;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

/** Proyecciones de lectura: SQL fijo, parámetros enlazados y paginación en PostgreSQL. */
@Repository
@RequiredArgsConstructor
public class ConsultaRepository {

  private final EntityManager em;
  private final ConsultaSql sql;

  public PaginaResponse<Map<String, Object>> listar(
    String dominio,
    Map<String, String> filtros,
    int pagina,
    int tamano
  ) {
    var parametros = new HashMap<String, Object>();
    StringBuilder where = new StringBuilder(" where 1=1");
    String campos = switch (dominio) {
      case "asignaturas" -> "concat(nombre,' ',servicio)";
      case "coordinadores" -> "concat(nombre,' ',correo,' ',servicio)";
      case "programas" -> "concat(nombre,' ',universidad)";
      case "residentes" -> "concat(nombre,' ',documento)";
      case "usuarios" -> "concat(nombre,' ',correo)";
      case "historial" -> "concat(usuario,' ',modulo,' ',accion,' ',detalle)";
      case "servicios" -> "concat(nombre,' ',descripcion)";
      case "universidades" -> "concat(nombre,' ',pais,' ',perfil)";
      case "rotaciones" -> "concat(residente,' ',universidad,' ',servicio,' ',asignatura)";
      default -> "nombre";
    };
    filtros.forEach((clave, valor) -> {
      switch (clave) {
        case "servicioId" -> where.append(" and \"servicioId\"=:servicioId");
        case "universidadId" -> where.append(
          " and \"universidadId\"=:universidadId"
        );
        case "q" -> where.append(
          " and position(lower(:q) in lower(" + campos + "))>0"
        );
        case "desde" -> where.append(
          " and cast(fecha as timestamp with time zone)>=:desde"
        );
        case "hasta" -> where.append(
          " and cast(fecha as timestamp with time zone)<:hasta"
        );
        case "periodo" -> {
          if (dominio.equals("residentes")) where.append(
            " and position(concat(', ',:periodo,', ') in concat(', ',periodo,', '))>0"
          );
          else where.append(
            " and extract(year from inicio)::int::text || '-' || case when extract(month from inicio)<=6 then '1' else '2' end = :periodo"
          );
        }
        case "proximas" -> where.append(
          " and inicio>(current_timestamp at time zone 'America/Bogota')::date and inicio<=(current_timestamp at time zone 'America/Bogota')::date+30 and estado<>'Cancelado'"
        );
        default -> where.append(
          " and lower(" + clave + ")=lower(:" + clave + ")"
        );
      }
      if (!clave.equals("proximas")) parametros.put(
        clave,
        clave.equals("universidadId") || clave.equals("servicioId")
          ? Integer.valueOf(valor)
          : clave.equals("desde") || clave.equals("hasta")
            ? java.time.LocalDate.parse(valor)
                .plusDays(clave.equals("hasta") ? 1 : 0)
                .atStartOfDay(java.time.ZoneId.of("America/Bogota"))
                .toOffsetDateTime()
            : valor
      );
    });
    String consulta = " from (" + base(dominio) + ") datos" + where;
    long total = (
      (Number) sql.escalar("select count(*)" + consulta, parametros)
    ).longValue();
    int paginas = Math.max(1, (int) Math.ceil((double) total / tamano));
    int actual = Math.min(pagina, paginas);
    String orden = dominio.equals("historial")
      ? "cast(fecha as timestamp with time zone) desc,id desc"
      : dominio.equals("rotaciones")
        ? "inicio,id"
        : "lower(nombre),id";
    var query = em.createNativeQuery(
      "select *" + consulta + " order by " + orden
    );
    parametros.forEach(query::setParameter);
    query.setFirstResult((actual - 1) * tamano).setMaxResults(tamano);
    return new PaginaResponse<>(
      sql.mapas(query.unwrap(NativeQuery.class)),
      total,
      paginas,
      actual,
      tamano
    );
  }

  public Map<String, List<String>> catalogos() {
    Map<String, List<String>> result = new LinkedHashMap<>();
    Map<String, String> sql = Map.of(
      "universidades",
      "select nombre_universidad as nombre from universidad where estado<>'Eliminado' order by nombre",
      "programas",
      "select distinct nombre_programa as nombre from programa where estado<>'Eliminado' order by nombre",
      "servicios",
      "select nombre from servicio where estado<>'Eliminado' order by nombre",
      "roles",
      "select nombre_rol as nombre from rol order by nombre",
      "modulos",
      "select distinct entidad_modificada as nombre from historial_auditoria order by nombre",
      "acciones",
      "select distinct accion_realizada as nombre from historial_auditoria order by nombre",
      "periodos",
      "select distinct " +
        PERIODO +
        " as nombre from rotacion order by nombre desc"
    );
    sql.forEach((key, value) ->
      result.put(
        key,
        this.sql
          .consultar(value, Map.of())
          .stream()
          .map(row -> row.get("nombre").toString())
          .toList()
      )
    );
    return result;
  }
}
