package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.dto.request.RotacionRequest;
import co.edu.javeriana.husi.dto.response.PaginaResponse;
import co.edu.javeriana.husi.model.entity.RotacionId;
import co.edu.javeriana.husi.repository.support.ConsultaSql;
import jakarta.persistence.EntityManager;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RotacionConsultaRepository {

  private final ConsultaSql consultas;
  private final EntityManager em;
  private static final String SELECT = """
  select r.id_asignatura as "asignaturaId",r.fecha_inicio as inicio,r.fecha_fin as fin,
  a.nombre_asignatura as asignatura,s.nombre as servicio,s.id_servicio as "servicioId",
  r.activa,r.estado as disponibilidad,r.cupo_disponible as "cupoDisponible",
  (select count(*) from rotacion_residente rr where rr.id_asignatura=r.id_asignatura and rr.fecha_inicio=r.fecha_inicio and rr.fecha_fin=r.fecha_fin) as relaciones
  from rotacion r join asignatura a on a.id_asignatura=r.id_asignatura join servicio s on s.id_servicio=a.id_servicio
  """;

  public PaginaResponse<Map<String, Object>> listar(
    String q,
    Boolean activa,
    Integer servicio,
    int pagina,
    int tamano
  ) {
    Map<String, Object> params = new HashMap<>();
    params.put("q", q);
    String where =
      " where position(lower(:q) in lower(concat(asignatura,' ',servicio)))>0";
    if (activa != null) {
      where += " and activa=:activa";
      params.put("activa", activa);
    }
    if (servicio != null) {
      where += " and \"servicioId\"=:servicio";
      params.put("servicio", servicio);
    }
    String from = " from (" + SELECT + ") datos" + where;
    long total = (
      (Number) consultas.escalar("select count(*)" + from, params)
    ).longValue();
    int paginas = Math.max(1, (int) Math.ceil((double) total / tamano)),
      actual = Math.min(pagina, paginas);
    params.put("limite", tamano);
    params.put("offset", (actual - 1) * tamano);
    return new PaginaResponse<>(
      consultas.consultar(
        "select *" +
          from +
          " order by inicio desc,fin desc,\"asignaturaId\" limit :limite offset :offset",
        params
      ),
      total,
      paginas,
      actual,
      tamano
    );
  }

  public Map<String, Object> detalle(RotacionId id) {
    return consultas
      .consultar(
        SELECT +
          " where r.id_asignatura=:id and r.fecha_inicio=:inicio and r.fecha_fin=:fin",
        params(id)
      )
      .getFirst();
  }

  private Map<String, Object> params(RotacionId id) {
    return Map.of(
      "id",
      id.getAsignaturaId(),
      "inicio",
      id.getInicio(),
      "fin",
      id.getFin()
    );
  }

  public long relaciones(RotacionId id) {
    return (
      (Number) consultas.escalar(
        "select count(*) from rotacion_residente where id_asignatura=:id and fecha_inicio=:inicio and fecha_fin=:fin",
        params(id)
      )
    ).longValue();
  }

  public void insertar(co.edu.javeriana.husi.model.entity.Rotacion rotacion) {
    em.persist(rotacion);
    em.flush();
  }

  public void cambiarClave(RotacionId id, RotacionRequest datos) {
    var query = em.createNativeQuery(
      "update rotacion set id_asignatura=:nuevo,fecha_inicio=:desde,fecha_fin=:hasta where id_asignatura=:id and fecha_inicio=:inicio and fecha_fin=:fin"
    );
    params(id).forEach(query::setParameter);
    query
      .setParameter("nuevo", datos.asignaturaId())
      .setParameter("desde", datos.inicio())
      .setParameter("hasta", datos.fin())
      .executeUpdate();
    em.clear();
  }

  public List<Map<String, Object>> asignaturas() {
    return consultas.consultar(
      "select a.id_asignatura as id,a.nombre_asignatura as nombre,s.nombre as servicio,s.id_servicio as \"servicioId\" from asignatura a join servicio s on s.id_servicio=a.id_servicio where a.estado='Activo' and s.estado='Activo' order by s.nombre,a.nombre_asignatura",
      Map.of()
    );
  }

  public List<Map<String, Object>> servicios() {
    return consultas.consultar(
      "select id_servicio as id,nombre from servicio where estado<>'Eliminado' order by nombre",
      Map.of()
    );
  }

  public boolean asignaturaActiva(Integer id) {
    return (
      (
        (Number) consultas.escalar(
          "select count(*) from asignatura a join servicio s on s.id_servicio=a.id_servicio where a.id_asignatura=:id and a.estado='Activo' and s.estado='Activo'",
          Map.of("id", id)
        )
      ).intValue() == 1
    );
  }

  public List<Map<String, Object>> resolver(
    String asignatura,
    String servicio
  ) {
    return consultas.consultar(
      "select a.id_asignatura as id from asignatura a join servicio s on s.id_servicio=a.id_servicio where lower(a.nombre_asignatura)=lower(:a) and lower(s.nombre)=lower(:s) and a.estado='Activo' and s.estado='Activo'",
      Map.of("a", asignatura, "s", servicio)
    );
  }
}
