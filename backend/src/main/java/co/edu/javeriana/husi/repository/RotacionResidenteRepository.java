package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.model.entity.RotacionId;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RotacionResidenteRepository {

  private final JdbcTemplate jdbc;

  /** Con {@code espera}: solo «Esperando cupo» por orden de llegada; si no, el resto por nombre. */
  public PaginaResponse<ResidenteRotacionResponse> listar(
    RotacionId id,
    int pagina,
    boolean espera
  ) {
    String filtro = espera
      ? " and rr.estado='Esperando cupo'"
      : " and rr.estado<>'Esperando cupo'";
    String orden = espera
      ? " order by rr.fecha_registro,rr.id_residente"
      : " order by lower(r.nombre_completo),rr.id_residente";
    var args = new Object[] {
      id.getAsignaturaId(),
      id.getInicio(),
      id.getFin(),
    };
    long total = jdbc.queryForObject(
      "select count(*) from rotacion_residente rr where rr.id_asignatura=? and rr.fecha_inicio=? and rr.fecha_fin=?" +
        filtro,
      Long.class,
      args
    );
    int paginas = Math.max(1, (int) Math.ceil(total / 10.0));
    int actual = Math.min(pagina, paginas);
    var filas = jdbc.query(
      "select rr.id_residente,r.nombre_completo,concat(td.codigo,' ',r.numero_documento) as documento,p.nombre_programa,rr.estado from rotacion_residente rr join residente r using(id_residente) join tipo_documento td on td.id_tipo_documento=r.id_tipo_documento join programa p on p.id_programa=r.id_programa where rr.id_asignatura=? and rr.fecha_inicio=? and rr.fecha_fin=?" +
        filtro +
        orden +
        " limit 10 offset ?",
      (rs, n) ->
        new ResidenteRotacionResponse(
          rs.getInt(1),
          rs.getString(2),
          rs.getString(3),
          rs.getString(4),
          rs.getString(5)
        ),
      id.getAsignaturaId(),
      id.getInicio(),
      id.getFin(),
      (actual - 1) * 10
    );
    return new PaginaResponse<>(filas, total, paginas, actual, 10);
  }

  public List<CatalogoResponse> candidatos(RotacionId id, String q) {
    return jdbc.query(
      "select r.id_residente,concat(r.nombre_completo,' — ',td.codigo,' ',r.numero_documento) from residente r join tipo_documento td on td.id_tipo_documento=r.id_tipo_documento where r.estado='Activo' and position(lower(?) in lower(concat(r.nombre_completo,' ',r.numero_documento)))>0 and not exists(select 1 from rotacion_residente rr where rr.id_residente=r.id_residente and rr.id_asignatura=? and rr.fecha_inicio=? and rr.fecha_fin=?) order by lower(r.nombre_completo),r.id_residente limit 30",
      (rs, n) -> new CatalogoResponse(rs.getInt(1), rs.getString(2)),
      q,
      id.getAsignaturaId(),
      id.getInicio(),
      id.getFin()
    );
  }

  public int cupos(RotacionId id) {
    return jdbc.queryForObject(
      "select cupo_disponible from rotacion where id_asignatura=? and fecha_inicio=? and fecha_fin=?",
      Integer.class,
      id.getAsignaturaId(),
      id.getInicio(),
      id.getFin()
    );
  }

  public boolean existe(RotacionId id, Integer residente) {
    return Boolean.TRUE.equals(
      jdbc.queryForObject(
        "select exists(select 1 from rotacion_residente where id_asignatura=? and fecha_inicio=? and fecha_fin=? and id_residente=?)",
        Boolean.class,
        id.getAsignaturaId(),
        id.getInicio(),
        id.getFin(),
        residente
      )
    );
  }

  public String estado(RotacionId id, Integer residente) {
    return jdbc.queryForObject(
      "select estado from rotacion_residente where id_asignatura=? and fecha_inicio=? and fecha_fin=? and id_residente=?",
      String.class,
      id.getAsignaturaId(),
      id.getInicio(),
      id.getFin(),
      residente
    );
  }

  public void insertar(RotacionId id, Integer residente) {
    jdbc.update(
      "insert into rotacion_residente(id_residente,id_asignatura,fecha_inicio,fecha_fin) values (?,?,?,?)",
      residente,
      id.getAsignaturaId(),
      id.getInicio(),
      id.getFin()
    );
  }

  /** Esperas en otras rotaciones del mismo servicio, en orden de llegada. */
  public List<EsperaServicioResponse> espera(RotacionId id) {
    return jdbc.query(
      "select rr.id_residente,r.nombre_completo,concat(td.codigo,' ',r.numero_documento),rr.id_asignatura,a.nombre_asignatura,rr.fecha_inicio,rr.fecha_fin," +
        "exists(select 1 from rotacion_residente o where o.id_residente=rr.id_residente and o.estado='Asignado' and o.fecha_inicio<=? and o.fecha_fin>=?) " +
        "from rotacion_residente rr join residente r on r.id_residente=rr.id_residente join tipo_documento td on td.id_tipo_documento=r.id_tipo_documento join asignatura a on a.id_asignatura=rr.id_asignatura " +
        "where rr.estado='Esperando cupo' and r.estado='Activo' and a.id_servicio=(select id_servicio from asignatura where id_asignatura=?) " +
        "and not exists(select 1 from rotacion_residente t where t.id_residente=rr.id_residente and t.id_asignatura=? and t.fecha_inicio=? and t.fecha_fin=?) " +
        "order by rr.fecha_registro,rr.id_residente limit 200",
      (rs, n) ->
        new EsperaServicioResponse(
          rs.getInt(1),
          rs.getString(2),
          rs.getString(3),
          rs.getInt(4),
          rs.getString(5),
          rs.getObject(6, java.time.LocalDate.class),
          rs.getObject(7, java.time.LocalDate.class),
          rs.getBoolean(8)
        ),
      id.getFin(),
      id.getInicio(),
      id.getAsignaturaId(),
      id.getAsignaturaId(),
      id.getInicio(),
      id.getFin()
    );
  }

  /**
   * Mueve una espera del mismo servicio a la rotación de destino como Asignado;
   * el trigger descuenta el cupo. Devuelve 0 si ya no estaba esperando.
   */
  public int trasladar(
    RotacionId origen,
    RotacionId destino,
    Integer residente
  ) {
    return jdbc.update(
      "update rotacion_residente set id_asignatura=?,fecha_inicio=?,fecha_fin=?,estado='Asignado' " +
        "where id_residente=? and id_asignatura=? and fecha_inicio=? and fecha_fin=? and estado='Esperando cupo' " +
        "and (select id_servicio from asignatura where id_asignatura=?)=(select id_servicio from asignatura where id_asignatura=?)",
      destino.getAsignaturaId(),
      destino.getInicio(),
      destino.getFin(),
      residente,
      origen.getAsignaturaId(),
      origen.getInicio(),
      origen.getFin(),
      origen.getAsignaturaId(),
      destino.getAsignaturaId()
    );
  }

  public void eliminar(RotacionId id, Integer residente) {
    jdbc.update(
      "delete from rotacion_residente where id_asignatura=? and fecha_inicio=? and fecha_fin=? and id_residente=?",
      id.getAsignaturaId(),
      id.getInicio(),
      id.getFin(),
      residente
    );
  }

  /** Volver a «Esperando cupo» lo pone al final de la lista (nueva fecha de registro). */
  public void cambiarEstado(RotacionId id, Integer residente, String estado) {
    jdbc.update(
      "update rotacion_residente set estado=?,fecha_registro=case when ?='Esperando cupo' then ? else fecha_registro end where id_asignatura=? and fecha_inicio=? and fecha_fin=? and id_residente=?",
      estado,
      estado,
      java.time.OffsetDateTime.now(),
      id.getAsignaturaId(),
      id.getInicio(),
      id.getFin(),
      residente
    );
  }
}
