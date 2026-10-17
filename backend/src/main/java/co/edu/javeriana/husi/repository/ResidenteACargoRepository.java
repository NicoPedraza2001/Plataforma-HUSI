package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.dto.response.ResidenteACargoResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Lectura del panel del coordinador: quién rota hoy en su servicio y quién llega después. */
@Repository
@RequiredArgsConstructor
public class ResidenteACargoRepository {

  /**
   * Residentes activos con asignación «Asignado» en una rotación activa del servicio. Cada
   * consulta añade su condición de fechas y su orden. Un residente aparece una vez por rotación.
   */
  private static final String SELECCION =
    "select r.id_residente,r.nombre_completo,concat(td.codigo,' ',r.numero_documento),u.nombre_universidad,p.nombre_programa,a.nombre_asignatura,rr.fecha_inicio,rr.fecha_fin " +
    "from rotacion_residente rr " +
    "join rotacion ro on ro.id_asignatura=rr.id_asignatura and ro.fecha_inicio=rr.fecha_inicio and ro.fecha_fin=rr.fecha_fin " +
    "join asignatura a on a.id_asignatura=rr.id_asignatura " +
    "join residente r on r.id_residente=rr.id_residente " +
    "join tipo_documento td on td.id_tipo_documento=r.id_tipo_documento " +
    "join programa p on p.id_programa=r.id_programa " +
    "join universidad u on u.id_universidad=p.id_universidad " +
    "where a.id_servicio=? and rr.estado='Asignado' and ro.activa and r.estado='Activo' ";

  private static final RowMapper<ResidenteACargoResponse> FILA = (resultado, numeroFila) ->
    new ResidenteACargoResponse(
      resultado.getInt(1),
      resultado.getString(2),
      resultado.getString(3),
      resultado.getString(4),
      resultado.getString(5),
      resultado.getString(6),
      resultado.getObject(7, LocalDate.class),
      resultado.getObject(8, LocalDate.class)
    );

  private final JdbcTemplate jdbc;

  /** Rotaciones cuya vigencia incluye {@code fecha}; primero las que terminan antes. */
  public List<ResidenteACargoResponse> listarEnCurso(Integer servicioId, LocalDate fecha) {
    return jdbc.query(
      SELECCION +
        "and rr.fecha_inicio<=? and rr.fecha_fin>=? " +
        "order by rr.fecha_fin,lower(r.nombre_completo),r.id_residente",
      FILA,
      servicioId,
      fecha,
      fecha
    );
  }

  /** Rotaciones que empiezan después de {@code fecha}; primero las que arrancan antes. */
  public List<ResidenteACargoResponse> listarProximas(Integer servicioId, LocalDate fecha) {
    return jdbc.query(
      SELECCION +
        "and rr.fecha_inicio>? " +
        "order by rr.fecha_inicio,lower(r.nombre_completo),r.id_residente",
      FILA,
      servicioId,
      fecha
    );
  }
}
