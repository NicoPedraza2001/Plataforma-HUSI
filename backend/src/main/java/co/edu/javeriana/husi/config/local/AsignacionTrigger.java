package co.edu.javeriana.husi.config.local;

import java.sql.*;
import java.util.Objects;
import org.h2.api.Trigger;

/** Equivalente local del ciclo de cupos de PostgreSQL. Participa en la misma transacción. */
public class AsignacionTrigger implements Trigger {

  // Posición de cada columna de rotacion_residente en las filas que entrega H2.
  private static final int ID_RESIDENTE = 0;
  private static final int ID_ASIGNATURA = 1;
  private static final int FECHA_INICIO = 2;
  private static final int FECHA_FIN = 3;
  private static final int ESTADO = 4;

  /** Indica si la asignación pasó a otra rotación (cambió asignatura o fechas). */
  private boolean cambioDeRotacion(Object[] filaAnterior, Object[] filaNueva) {
    return (
      !Objects.equals(filaAnterior[ID_ASIGNATURA], filaNueva[ID_ASIGNATURA]) ||
      !Objects.equals(filaAnterior[FECHA_INICIO], filaNueva[FECHA_INICIO]) ||
      !Objects.equals(filaAnterior[FECHA_FIN], filaNueva[FECHA_FIN])
    );
  }

  /**
   * H2 entrega {@code filaAnterior} null en un insert y {@code filaNueva} null en un delete.
   * Al insertar, el estado se decide aquí: Asignado si hay cupo o Esperando cupo si no.
   */
  public void fire(Connection conexion, Object[] filaAnterior, Object[] filaNueva)
    throws SQLException {
    boolean hayCambio =
      filaAnterior == null ||
      filaNueva == null ||
      cambioDeRotacion(filaAnterior, filaNueva) ||
      !Objects.equals(filaAnterior[ESTADO], filaNueva[ESTADO]);
    if (!hayCambio) return;
    if (filaNueva != null && !"Cancelado".equals(filaNueva[ESTADO])) {
      validarRotacionActiva(conexion, filaNueva);
    }
    if (filaAnterior == null) {
      validarResidenteActivo(conexion, filaNueva[ID_RESIDENTE]);
      if (!"Cancelado".equals(filaNueva[ESTADO])) filaNueva[ESTADO] =
        ajustarCupo(conexion, filaNueva, -1) ? "Asignado" : "Esperando cupo";
      return;
    }
    boolean liberaCupo =
      "Asignado".equals(filaAnterior[ESTADO]) &&
      (filaNueva == null ||
        cambioDeRotacion(filaAnterior, filaNueva) ||
        !"Asignado".equals(filaNueva[ESTADO]));
    if (liberaCupo) {
      ajustarCupo(conexion, filaAnterior, 1);
      ListaEspera.promover(
        conexion,
        filaAnterior[ID_ASIGNATURA],
        filaAnterior[FECHA_INICIO],
        filaAnterior[FECHA_FIN]
      );
    }
    boolean ocupaCupo =
      filaNueva != null &&
      "Asignado".equals(filaNueva[ESTADO]) &&
      (cambioDeRotacion(filaAnterior, filaNueva) ||
        !"Asignado".equals(filaAnterior[ESTADO]));
    if (ocupaCupo && !ajustarCupo(conexion, filaNueva, -1)) throw new SQLException(
      "No hay cupo disponible",
      "23514"
    );
  }

  private void validarRotacionActiva(Connection conexion, Object[] fila)
    throws SQLException {
    try (
      var consulta = conexion.prepareStatement(
        "select activa from rotacion where id_asignatura=? and fecha_inicio=? and fecha_fin=? for update"
      )
    ) {
      asignarClaveRotacion(consulta, fila);
      try (var resultado = consulta.executeQuery()) {
        if (!resultado.next()) throw new SQLException(
          "Rotación inexistente",
          "23503"
        );
        if (!resultado.getBoolean(1)) throw new SQLException(
          "Rotación inactiva",
          "23514"
        );
      }
    }
  }

  private void validarResidenteActivo(Connection conexion, Object residenteId)
    throws SQLException {
    try (
      var consulta = conexion.prepareStatement(
        "select estado from residente where id_residente=?"
      )
    ) {
      consulta.setObject(1, residenteId);
      try (var resultado = consulta.executeQuery()) {
        if (
          !resultado.next() || !"Activo".equals(resultado.getString(1))
        ) throw new SQLException("Residente inexistente o inactivo", "23514");
      }
    }
  }

  private void asignarClaveRotacion(PreparedStatement consulta, Object[] fila)
    throws SQLException {
    consulta.setObject(1, fila[ID_ASIGNATURA]);
    consulta.setObject(2, fila[FECHA_INICIO]);
    consulta.setObject(3, fila[FECHA_FIN]);
  }

  /**
   * Suma {@code variacion} al cupo disponible de la rotación de la fila. Devuelve false, sin
   * modificar nada, si el cupo quedaría negativo.
   */
  private boolean ajustarCupo(Connection conexion, Object[] fila, int variacion)
    throws SQLException {
    try (
      var consulta = conexion.prepareStatement(
        "select cupo_disponible from rotacion where id_asignatura=? and fecha_inicio=? and fecha_fin=? for update"
      )
    ) {
      asignarClaveRotacion(consulta, fila);
      try (var resultado = consulta.executeQuery()) {
        if (!resultado.next()) throw new SQLException(
          "Rotación inexistente",
          "23503"
        );
        int nuevoCupo = resultado.getInt(1) + variacion;
        if (nuevoCupo < 0) return false;
        CupoRotacionTrigger.AJUSTANDO.set(true);
        try (
          var actualizacion = conexion.prepareStatement(
            "update rotacion set cupo_disponible=?,estado=? where id_asignatura=? and fecha_inicio=? and fecha_fin=?"
          )
        ) {
          actualizacion.setInt(1, nuevoCupo);
          actualizacion.setString(2, nuevoCupo == 0 ? "Cupo Lleno" : "Disponible");
          actualizacion.setObject(3, fila[ID_ASIGNATURA]);
          actualizacion.setObject(4, fila[FECHA_INICIO]);
          actualizacion.setObject(5, fila[FECHA_FIN]);
          actualizacion.executeUpdate();
        } finally {
          CupoRotacionTrigger.AJUSTANDO.remove();
        }
      }
    }
    return true;
  }
}
