package co.edu.javeriana.husi.config.local;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

/** Exclusivo de H2: equivalente de promover_espera_rotacion de PostgreSQL. */
final class ListaEspera {

  private ListaEspera() {}

  /** Asigna por orden de llegada a los residentes en espera mientras haya cupo. */
  static void promover(
    Connection conexion,
    Object asignaturaId,
    Object inicio,
    Object fin
  ) throws SQLException {
    int cupoDisponible;
    try (
      var consulta = conexion.prepareStatement(
        "select r.cupo_disponible from rotacion r join asignatura a on a.id_asignatura=r.id_asignatura join servicio s on s.id_servicio=a.id_servicio " +
          "where r.id_asignatura=? and r.fecha_inicio=? and r.fecha_fin=? and r.activa and a.estado='Activo' and s.estado='Activo' and r.fecha_fin>=?"
      )
    ) {
      consulta.setObject(1, asignaturaId);
      consulta.setObject(2, inicio);
      consulta.setObject(3, fin);
      consulta.setObject(4, LocalDate.now(ZoneId.of("America/Bogota")));
      try (var resultado = consulta.executeQuery()) {
        if (!resultado.next()) return;
        cupoDisponible = resultado.getInt(1);
      }
    }
    if (cupoDisponible <= 0) return;
    var residentesAPromover = new ArrayList<Integer>();
    try (
      var consulta = conexion.prepareStatement(
        "select rr.id_residente from rotacion_residente rr join residente res on res.id_residente=rr.id_residente " +
          "where rr.id_asignatura=? and rr.fecha_inicio=? and rr.fecha_fin=? and rr.estado='Esperando cupo' and res.estado='Activo' " +
          "order by rr.fecha_registro,rr.id_residente limit ?"
      )
    ) {
      consulta.setObject(1, asignaturaId);
      consulta.setObject(2, inicio);
      consulta.setObject(3, fin);
      consulta.setInt(4, cupoDisponible);
      try (var resultado = consulta.executeQuery()) {
        while (resultado.next()) residentesAPromover.add(resultado.getInt(1));
      }
    }
    // AsignacionTrigger descuenta el cupo de cada promovido.
    try (
      var actualizacion = conexion.prepareStatement(
        "update rotacion_residente set estado='Asignado' where id_residente=? and id_asignatura=? and fecha_inicio=? and fecha_fin=?"
      )
    ) {
      for (Integer residenteId : residentesAPromover) {
        actualizacion.setInt(1, residenteId);
        actualizacion.setObject(2, asignaturaId);
        actualizacion.setObject(3, inicio);
        actualizacion.setObject(4, fin);
        actualizacion.executeUpdate();
      }
    }
  }
}
