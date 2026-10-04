package co.edu.javeriana.husi.config.local;

import java.sql.*;
import java.util.Objects;
import org.h2.api.Trigger;

/** Exclusivo de H2: capacidad inicial del servicio y protección de cambios directos. */
public class CupoRotacionTrigger implements Trigger {

  static final ThreadLocal<Boolean> AJUSTANDO = ThreadLocal.withInitial(
    () -> false
  );

  public void fire(Connection c, Object[] anterior, Object[] nuevo)
    throws SQLException {
    if (anterior != null) {
      if (
        !Objects.equals(anterior[3], nuevo[3]) && !AJUSTANDO.get()
      ) throw new SQLException(
        "El cupo se ajusta mediante las asignaciones.",
        "23514"
      );
      return;
    }
    try (
      var p = c.prepareStatement(
        "select s.cupo_simultaneo,a.estado,s.estado from asignatura a join servicio s using(id_servicio) where a.id_asignatura=?"
      )
    ) {
      p.setObject(1, nuevo[0]);
      try (var r = p.executeQuery()) {
        if (!r.next()) throw new SQLException(
          "Asignatura inexistente",
          "23503"
        );
        if (
          !"Activo".equals(r.getString(2)) || !"Activo".equals(r.getString(3))
        ) throw new SQLException("Asignatura o servicio inactivo", "23514");
        nuevo[3] = r.getInt(1);
        nuevo[4] = r.getInt(1) == 0 ? "Cupo Lleno" : "Disponible";
      }
    }
  }
}
