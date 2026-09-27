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

  public PaginaResponse<ResidenteRotacionResponse> listar(
    RotacionId id,
    int pagina
  ) {
    var args = new Object[] {
      id.getAsignaturaId(),
      id.getInicio(),
      id.getFin(),
    };
    long total = jdbc.queryForObject(
      "select count(*) from rotacion_residente where id_asignatura=? and fecha_inicio=? and fecha_fin=?",
      Long.class,
      args
    );
    int paginas = Math.max(1, (int) Math.ceil(total / 10.0));
    int actual = Math.min(pagina, paginas);
    var filas = jdbc.query(
      "select rr.id_residente,r.nombre_completo,concat(r.tipo_documento,' ',r.numero_documento) as documento,p.nombre_programa,rr.estado from rotacion_residente rr join residente r using(id_residente) join programa p using(id_programa) where rr.id_asignatura=? and rr.fecha_inicio=? and rr.fecha_fin=? order by lower(r.nombre_completo),rr.id_residente limit 10 offset ?",
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
      "select r.id_residente,concat(r.nombre_completo,' — ',r.tipo_documento,' ',r.numero_documento) from residente r where r.estado='Activo' and strpos(lower(concat(r.nombre_completo,' ',r.numero_documento)),lower(?))>0 and not exists(select 1 from rotacion_residente rr where rr.id_residente=r.id_residente and rr.id_asignatura=? and rr.fecha_inicio=? and rr.fecha_fin=?) order by lower(r.nombre_completo),r.id_residente limit 30",
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

  public void cambiarEstado(RotacionId id, Integer residente, String estado) {
    jdbc.update(
      "update rotacion_residente set estado=? where id_asignatura=? and fecha_inicio=? and fecha_fin=? and id_residente=?",
      estado,
      id.getAsignaturaId(),
      id.getInicio(),
      id.getFin(),
      residente
    );
  }
}
