package co.edu.javeriana.husi.config.local;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

/** Exclusivo de H2: equivalente de promover_espera_rotacion de PostgreSQL. */
final class ListaEspera {

  private ListaEspera() {}

  /** Asigna por orden de llegada a los residentes en espera mientras haya cupo. */
  static void promover(Connection c, Object asignatura, Object inicio, Object fin)
    throws SQLException {
    int cupo;
    try (
      var p = c.prepareStatement(
        "select r.cupo_disponible from rotacion r join asignatura a on a.id_asignatura=r.id_asignatura join servicio s on s.id_servicio=a.id_servicio " +
          "where r.id_asignatura=? and r.fecha_inicio=? and r.fecha_fin=? and r.activa and a.estado='Activo' and s.estado='Activo' and r.fecha_fin>=?"
      )
    ) {
      p.setObject(1, asignatura);
      p.setObject(2, inicio);
      p.setObject(3, fin);
      p.setObject(4, LocalDate.now(ZoneId.of("America/Bogota")));
      try (var r = p.executeQuery()) {
        if (!r.next()) return;
        cupo = r.getInt(1);
      }
    }
    if (cupo <= 0) return;
    var espera = new ArrayList<Integer>();
    try (
      var p = c.prepareStatement(
        "select rr.id_residente from rotacion_residente rr join residente res on res.id_residente=rr.id_residente " +
          "where rr.id_asignatura=? and rr.fecha_inicio=? and rr.fecha_fin=? and rr.estado='Esperando cupo' and res.estado='Activo' " +
          "order by rr.fecha_registro,rr.id_residente limit ?"
      )
    ) {
      p.setObject(1, asignatura);
      p.setObject(2, inicio);
      p.setObject(3, fin);
      p.setInt(4, cupo);
      try (var r = p.executeQuery()) {
        while (r.next()) espera.add(r.getInt(1));
      }
    }
    // AsignacionTrigger descuenta el cupo de cada promovido.
    try (
      var u = c.prepareStatement(
        "update rotacion_residente set estado='Asignado' where id_residente=? and id_asignatura=? and fecha_inicio=? and fecha_fin=?"
      )
    ) {
      for (Integer residente : espera) {
        u.setInt(1, residente);
        u.setObject(2, asignatura);
        u.setObject(3, inicio);
        u.setObject(4, fin);
        u.executeUpdate();
      }
    }
  }
}
