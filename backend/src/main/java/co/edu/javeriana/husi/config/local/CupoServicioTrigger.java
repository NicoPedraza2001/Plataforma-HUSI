package co.edu.javeriana.husi.config.local;

import java.sql.*;
import java.util.Objects;
import org.h2.api.Trigger;

/** Exclusivo de H2: equivalente de trg_servicio_recalcular_cupo de PostgreSQL (solo rotaciones vigentes). */
public class CupoServicioTrigger implements Trigger {

  private static final int ID = 0, CUPO_SIMULTANEO = 4;

  public void fire(Connection c, Object[] anterior, Object[] nuevo)
    throws SQLException {
    if (
      anterior == null ||
      nuevo == null ||
      Objects.equals(anterior[CUPO_SIMULTANEO], nuevo[CUPO_SIMULTANEO])
    ) return;
    int cupo = ((Number) nuevo[CUPO_SIMULTANEO]).intValue();
    var rotaciones = new java.util.ArrayList<Object[]>();
    try (
      var p = c.prepareStatement(
        "select r.id_asignatura,r.fecha_inicio,r.fecha_fin," +
          "(select count(*) from rotacion_residente rr where rr.id_asignatura=r.id_asignatura and rr.fecha_inicio=r.fecha_inicio and rr.fecha_fin=r.fecha_fin and rr.estado='Asignado') " +
          "from rotacion r join asignatura a on a.id_asignatura=r.id_asignatura where a.id_servicio=? and r.fecha_fin>=?"
      );
      var u = c.prepareStatement(
        "update rotacion set cupo_disponible=?,estado=? where id_asignatura=? and fecha_inicio=? and fecha_fin=?"
      )
    ) {
      p.setObject(1, nuevo[ID]);
      p.setObject(
        2,
        java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota"))
      );
      try (var r = p.executeQuery()) {
        while (r.next()) {
          int disponible = cupo - r.getInt(4);
          if (disponible < 0) throw new SQLException(
            "El cupo simultáneo no puede ser menor a los residentes asignados (" +
              r.getInt(4) +
              ").",
            "23514"
          );
          u.setInt(1, disponible);
          u.setString(2, disponible == 0 ? "Cupo Lleno" : "Disponible");
          u.setObject(3, r.getObject(1));
          u.setObject(4, r.getObject(2));
          u.setObject(5, r.getObject(3));
          u.addBatch();
          rotaciones.add(
            new Object[] { r.getObject(1), r.getObject(2), r.getObject(3) }
          );
        }
      }
      CupoRotacionTrigger.AJUSTANDO.set(true);
      try {
        u.executeBatch();
      } finally {
        CupoRotacionTrigger.AJUSTANDO.remove();
      }
    }
    if (cupo > ((Number) anterior[CUPO_SIMULTANEO]).intValue()) {
      for (Object[] k : rotaciones) ListaEspera.promover(c, k[0], k[1], k[2]);
    }
  }
}
