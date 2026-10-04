package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.dto.request.RotacionRequest;
import co.edu.javeriana.husi.dto.response.PaginaResponse;
import co.edu.javeriana.husi.model.entity.Rotacion;
import co.edu.javeriana.husi.model.entity.RotacionId;
import co.edu.javeriana.husi.repository.support.ConsultaSql;
import jakarta.persistence.EntityManager;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RotacionConsultaRepository {

  private final ConsultaSql consultaSql;
  private final EntityManager entityManager;
  private static final String SELECT_ROTACION = """
  select r.id_asignatura as "asignaturaId",r.fecha_inicio as inicio,r.fecha_fin as fin,
  a.nombre_asignatura as asignatura,s.nombre as servicio,s.id_servicio as "servicioId",
  r.activa,r.estado as disponibilidad,r.cupo_disponible as "cupoDisponible",
  (select count(*) from rotacion_residente rr where rr.id_asignatura=r.id_asignatura and rr.fecha_inicio=r.fecha_inicio and rr.fecha_fin=r.fecha_fin) as relaciones
  from rotacion r join asignatura a on a.id_asignatura=r.id_asignatura join servicio s on s.id_servicio=a.id_servicio
  """;

  public PaginaResponse<Map<String, Object>> listar(
    String busqueda,
    Boolean activa,
    Integer servicioId,
    int pagina,
    int tamano
  ) {
    Map<String, Object> parametros = new HashMap<>();
    parametros.put("q", busqueda);
    String where =
      " where position(lower(:q) in lower(concat(asignatura,' ',servicio)))>0";
    if (activa != null) {
      where += " and activa=:activa";
      parametros.put("activa", activa);
    }
    if (servicioId != null) {
      where += " and \"servicioId\"=:servicio";
      parametros.put("servicio", servicioId);
    }
    String from = " from (" + SELECT_ROTACION + ") datos" + where;
    long total = (
      (Number) consultaSql.escalar("select count(*)" + from, parametros)
    ).longValue();
    int totalPaginas = Math.max(1, (int) Math.ceil((double) total / tamano));
    int paginaActual = Math.min(pagina, totalPaginas);
    parametros.put("limite", tamano);
    parametros.put("offset", (paginaActual - 1) * tamano);
    return new PaginaResponse<>(
      consultaSql.consultar(
        "select *" +
          from +
          " order by inicio desc,fin desc,\"asignaturaId\" limit :limite offset :offset",
        parametros
      ),
      total,
      totalPaginas,
      paginaActual,
      tamano
    );
  }

  public Map<String, Object> detalle(RotacionId id) {
    return consultaSql
      .consultar(
        SELECT_ROTACION +
          " where r.id_asignatura=:id and r.fecha_inicio=:inicio and r.fecha_fin=:fin",
        parametrosDeId(id)
      )
      .getFirst();
  }

  /** Parámetros {@code :id}, {@code :inicio} y {@code :fin} de la clave compuesta. */
  private Map<String, Object> parametrosDeId(RotacionId id) {
    return Map.of(
      "id",
      id.getAsignaturaId(),
      "inicio",
      id.getInicio(),
      "fin",
      id.getFin()
    );
  }

  /** Residentes relacionados con la rotación en cualquier estado (incluidos cancelados). */
  public long contarResidentesRelacionados(RotacionId id) {
    return (
      (Number) consultaSql.escalar(
        "select count(*) from rotacion_residente where id_asignatura=:id and fecha_inicio=:inicio and fecha_fin=:fin",
        parametrosDeId(id)
      )
    ).longValue();
  }

  public void insertar(Rotacion rotacion) {
    entityManager.persist(rotacion);
    entityManager.flush();
  }

  /** Mueve la clave compuesta (asignatura y fechas) de la rotación {@code id} a {@code datos}. */
  public void cambiarFechas(RotacionId id, RotacionRequest datos) {
    var query = entityManager.createNativeQuery(
      "update rotacion set id_asignatura=:nuevaAsignatura,fecha_inicio=:nuevoInicio,fecha_fin=:nuevoFin where id_asignatura=:id and fecha_inicio=:inicio and fecha_fin=:fin"
    );
    parametrosDeId(id).forEach(query::setParameter);
    query
      .setParameter("nuevaAsignatura", datos.asignaturaId())
      .setParameter("nuevoInicio", datos.inicio())
      .setParameter("nuevoFin", datos.fin())
      .executeUpdate();
    entityManager.clear();
  }

  public List<Map<String, Object>> asignaturas() {
    return consultaSql.consultar(
      "select a.id_asignatura as id,a.nombre_asignatura as nombre,s.nombre as servicio,s.id_servicio as \"servicioId\" from asignatura a join servicio s on s.id_servicio=a.id_servicio where a.estado='Activo' and s.estado='Activo' order by s.nombre,a.nombre_asignatura",
      Map.of()
    );
  }

  public List<Map<String, Object>> servicios() {
    return consultaSql.consultar(
      "select id_servicio as id,nombre from servicio where estado<>'Eliminado' order by nombre",
      Map.of()
    );
  }

  public boolean asignaturaActiva(Integer asignaturaId) {
    return (
      (
        (Number) consultaSql.escalar(
          "select count(*) from asignatura a join servicio s on s.id_servicio=a.id_servicio where a.id_asignatura=:id and a.estado='Activo' and s.estado='Activo'",
          Map.of("id", asignaturaId)
        )
      ).intValue() == 1
    );
  }

  /** Asignaturas activas (de servicios activos) que coinciden por nombre, sin distinguir mayúsculas. */
  public List<Map<String, Object>> buscarAsignaturasActivas(
    String nombreAsignatura,
    String nombreServicio
  ) {
    return consultaSql.consultar(
      "select a.id_asignatura as id from asignatura a join servicio s on s.id_servicio=a.id_servicio where lower(a.nombre_asignatura)=lower(:asignatura) and lower(s.nombre)=lower(:servicio) and a.estado='Activo' and s.estado='Activo'",
      Map.of("asignatura", nombreAsignatura, "servicio", nombreServicio)
    );
  }
}
