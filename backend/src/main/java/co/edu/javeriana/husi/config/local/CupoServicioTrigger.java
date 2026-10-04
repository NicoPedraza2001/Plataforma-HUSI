package co.edu.javeriana.husi.config.local;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Objects;
import org.h2.api.Trigger;

/** Exclusivo de H2: equivalente de trg_servicio_recalcular_cupo de PostgreSQL (solo rotaciones vigentes). */
public class CupoServicioTrigger implements Trigger {

  // Posición de cada columna de servicio en las filas que entrega H2.
  private static final int ID_SERVICIO = 0;
  private static final int CUPO_SIMULTANEO = 4;

  /** Clave compuesta de una rotación: asignatura, fecha de inicio y fecha de fin. */
  private record ClaveRotacion(Object asignaturaId, Object inicio, Object fin) {}

  public void fire(Connection conexion, Object[] filaAnterior, Object[] filaNueva)
    throws SQLException {
    if (
      filaAnterior == null ||
      filaNueva == null ||
      Objects.equals(filaAnterior[CUPO_SIMULTANEO], filaNueva[CUPO_SIMULTANEO])
    ) return;
    int nuevoCupoSimultaneo = ((Number) filaNueva[CUPO_SIMULTANEO]).intValue();
    var rotacionesVigentes = new ArrayList<ClaveRotacion>();
    try (
      var consulta = conexion.prepareStatement(
        "select r.id_asignatura,r.fecha_inicio,r.fecha_fin," +
          "(select count(*) from rotacion_residente rr where rr.id_asignatura=r.id_asignatura and rr.fecha_inicio=r.fecha_inicio and rr.fecha_fin=r.fecha_fin and rr.estado='Asignado') " +
          "from rotacion r join asignatura a on a.id_asignatura=r.id_asignatura where a.id_servicio=? and r.fecha_fin>=?"
      );
      var actualizacion = conexion.prepareStatement(
        "update rotacion set cupo_disponible=?,estado=? where id_asignatura=? and fecha_inicio=? and fecha_fin=?"
      )
    ) {
      consulta.setObject(1, filaNueva[ID_SERVICIO]);
      consulta.setObject(2, LocalDate.now(ZoneId.of("America/Bogota")));
      try (var resultado = consulta.executeQuery()) {
        while (resultado.next()) {
          int asignados = resultado.getInt(4);
          int disponible = nuevoCupoSimultaneo - asignados;
          if (disponible < 0) throw new SQLException(
            "El cupo simultáneo no puede ser menor a los residentes asignados (" +
              asignados +
              ").",
            "23514"
          );
          var clave = new ClaveRotacion(
            resultado.getObject(1),
            resultado.getObject(2),
            resultado.getObject(3)
          );
          actualizacion.setInt(1, disponible);
          actualizacion.setString(2, disponible == 0 ? "Cupo Lleno" : "Disponible");
          actualizacion.setObject(3, clave.asignaturaId());
          actualizacion.setObject(4, clave.inicio());
          actualizacion.setObject(5, clave.fin());
          actualizacion.addBatch();
          rotacionesVigentes.add(clave);
        }
      }
      CupoRotacionTrigger.AJUSTANDO.set(true);
      try {
        actualizacion.executeBatch();
      } finally {
        CupoRotacionTrigger.AJUSTANDO.remove();
      }
    }
    boolean aumentoElCupo =
      nuevoCupoSimultaneo > ((Number) filaAnterior[CUPO_SIMULTANEO]).intValue();
    if (aumentoElCupo) {
      for (ClaveRotacion clave : rotacionesVigentes) ListaEspera.promover(
        conexion,
        clave.asignaturaId(),
        clave.inicio(),
        clave.fin()
      );
    }
  }
}
