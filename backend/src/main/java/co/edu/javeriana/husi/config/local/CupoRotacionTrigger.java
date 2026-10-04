package co.edu.javeriana.husi.config.local;

import java.sql.*;
import java.util.Objects;
import org.h2.api.Trigger;

/** Exclusivo de H2: capacidad inicial del servicio y protección de cambios directos. */
public class CupoRotacionTrigger implements Trigger {

  // Posición de cada columna de rotacion en las filas que entrega H2.
  private static final int ID_ASIGNATURA = 0;
  private static final int CUPO_DISPONIBLE = 3;
  private static final int ESTADO = 4;

  /** Lo activan los otros triggers mientras ajustan el cupo, para que este no lo bloquee. */
  static final ThreadLocal<Boolean> AJUSTANDO = ThreadLocal.withInitial(
    () -> false
  );

  public void fire(Connection conexion, Object[] filaAnterior, Object[] filaNueva)
    throws SQLException {
    if (filaAnterior != null) {
      if (
        !Objects.equals(
          filaAnterior[CUPO_DISPONIBLE],
          filaNueva[CUPO_DISPONIBLE]
        ) &&
        !AJUSTANDO.get()
      ) throw new SQLException(
        "El cupo se ajusta mediante las asignaciones.",
        "23514"
      );
      return;
    }
    try (
      var consulta = conexion.prepareStatement(
        "select s.cupo_simultaneo,a.estado,s.estado from asignatura a join servicio s using(id_servicio) where a.id_asignatura=?"
      )
    ) {
      consulta.setObject(1, filaNueva[ID_ASIGNATURA]);
      try (var resultado = consulta.executeQuery()) {
        if (!resultado.next()) throw new SQLException(
          "Asignatura inexistente",
          "23503"
        );
        String estadoAsignatura = resultado.getString(2);
        String estadoServicio = resultado.getString(3);
        if (
          !"Activo".equals(estadoAsignatura) || !"Activo".equals(estadoServicio)
        ) throw new SQLException("Asignatura o servicio inactivo", "23514");
        int cupoSimultaneo = resultado.getInt(1);
        filaNueva[CUPO_DISPONIBLE] = cupoSimultaneo;
        filaNueva[ESTADO] = cupoSimultaneo == 0 ? "Cupo Lleno" : "Disponible";
      }
    }
  }
}
