package co.edu.javeriana.husi.config.local;

import java.sql.*;
import java.util.Objects;
import org.h2.api.Trigger;

/** Equivalente local del ciclo de cupos de PostgreSQL. Participa en la misma transacción. */
public class AsignacionTrigger implements Trigger {

  private boolean cambio(Object[] a, Object[] b) {
    return (
      !Objects.equals(a[1], b[1]) ||
      !Objects.equals(a[2], b[2]) ||
      !Objects.equals(a[3], b[3])
    );
  }

  public void fire(Connection c, Object[] a, Object[] b) throws SQLException {
    boolean cambia =
      a == null || b == null || cambio(a, b) || !Objects.equals(a[4], b[4]);
    if (!cambia) return;
    if (b != null && !"Cancelado".equals(b[4])) {
      try (
        var p = c.prepareStatement(
          "select activa from rotacion where id_asignatura=? and fecha_inicio=? and fecha_fin=? for update"
        )
      ) {
        clave(p, b);
        try (var r = p.executeQuery()) {
          if (!r.next()) throw new SQLException(
            "Rotación inexistente",
            "23503"
          );
          if (!r.getBoolean(1)) throw new SQLException(
            "Rotación inactiva",
            "23514"
          );
        }
      }
    }
    if (a == null) {
      try (
        var p = c.prepareStatement(
          "select estado from residente where id_residente=?"
        )
      ) {
        p.setObject(1, b[0]);
        try (var r = p.executeQuery()) {
          if (
            !r.next() || !"Activo".equals(r.getString(1))
          ) throw new SQLException("Residente inexistente o inactivo", "23514");
        }
      }
      if (!"Cancelado".equals(b[4])) b[4] = ajustar(c, b, -1)
        ? "Asignado"
        : "Esperando cupo";
      return;
    }
    if (
      "Asignado".equals(a[4]) &&
      (b == null || cambio(a, b) || !"Asignado".equals(b[4]))
    ) ajustar(c, a, 1);
    if (
      b != null &&
      "Asignado".equals(b[4]) &&
      (cambio(a, b) || !"Asignado".equals(a[4])) &&
      !ajustar(c, b, -1)
    ) throw new SQLException("No hay cupo disponible", "23514");
  }

  private void clave(PreparedStatement p, Object[] fila) throws SQLException {
    p.setObject(1, fila[1]);
    p.setObject(2, fila[2]);
    p.setObject(3, fila[3]);
  }

  private boolean ajustar(Connection c, Object[] fila, int delta)
    throws SQLException {
    try (
      var p = c.prepareStatement(
        "select cupo_disponible from rotacion where id_asignatura=? and fecha_inicio=? and fecha_fin=? for update"
      )
    ) {
      clave(p, fila);
      try (var r = p.executeQuery()) {
        if (!r.next()) throw new SQLException("Rotación inexistente", "23503");
        int cupo = r.getInt(1) + delta;
        if (cupo < 0) return false;
        CupoRotacionTrigger.AJUSTANDO.set(true);
        try (
          var u = c.prepareStatement(
            "update rotacion set cupo_disponible=?,estado=? where id_asignatura=? and fecha_inicio=? and fecha_fin=?"
          )
        ) {
          u.setInt(1, cupo);
          u.setString(2, cupo == 0 ? "Cupo Lleno" : "Disponible");
          u.setObject(3, fila[1]);
          u.setObject(4, fila[2]);
          u.setObject(5, fila[3]);
          u.executeUpdate();
        } finally {
          CupoRotacionTrigger.AJUSTANDO.remove();
        }
      }
    }
    return true;
  }
}
