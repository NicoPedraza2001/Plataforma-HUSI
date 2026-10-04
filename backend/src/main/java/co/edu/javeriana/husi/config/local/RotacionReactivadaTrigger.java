package co.edu.javeriana.husi.config.local;

import java.sql.*;
import org.h2.api.Trigger;

/** Exclusivo de H2: equivalente de trg_rotacion_reactivada de PostgreSQL. */
public class RotacionReactivadaTrigger implements Trigger {

  private static final int ACTIVA = 5;

  public void fire(Connection c, Object[] anterior, Object[] nuevo)
    throws SQLException {
    if (
      anterior != null &&
      nuevo != null &&
      Boolean.FALSE.equals(anterior[ACTIVA]) &&
      Boolean.TRUE.equals(nuevo[ACTIVA])
    ) ListaEspera.promover(c, nuevo[0], nuevo[1], nuevo[2]);
  }
}
