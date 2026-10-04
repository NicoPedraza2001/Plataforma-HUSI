package co.edu.javeriana.husi.config.local;

import java.sql.*;
import org.h2.api.Trigger;

/** Exclusivo de H2: equivalente de trg_rotacion_reactivada de PostgreSQL. */
public class RotacionReactivadaTrigger implements Trigger {

  // Posición de cada columna de rotacion en las filas que entrega H2.
  private static final int ID_ASIGNATURA = 0;
  private static final int FECHA_INICIO = 1;
  private static final int FECHA_FIN = 2;
  private static final int ACTIVA = 5;

  public void fire(Connection conexion, Object[] filaAnterior, Object[] filaNueva)
    throws SQLException {
    boolean seReactivo =
      filaAnterior != null &&
      filaNueva != null &&
      Boolean.FALSE.equals(filaAnterior[ACTIVA]) &&
      Boolean.TRUE.equals(filaNueva[ACTIVA]);
    if (seReactivo) ListaEspera.promover(
      conexion,
      filaNueva[ID_ASIGNATURA],
      filaNueva[FECHA_INICIO],
      filaNueva[FECHA_FIN]
    );
  }
}
