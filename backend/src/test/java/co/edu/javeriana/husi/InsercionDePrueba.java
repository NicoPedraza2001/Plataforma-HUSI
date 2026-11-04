package co.edu.javeriana.husi;

import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * Inserta los datos que prepara un test y devuelve su id generado. Funciona igual en PostgreSQL y
 * en H2, a diferencia de {@code insert ... returning}, que H2 no admite.
 */
public final class InsercionDePrueba {

  private InsercionDePrueba() {}

  public static Integer insertarYObtenerId(
    JdbcTemplate jdbc,
    String columnaId,
    String insert,
    Object... parametros
  ) {
    var idGenerado = new GeneratedKeyHolder();
    jdbc.update(
      conexion -> {
        var sentencia = conexion.prepareStatement(insert, new String[] { columnaId });
        for (int i = 0; i < parametros.length; i++) sentencia.setObject(i + 1, parametros[i]);
        return sentencia;
      },
      idGenerado
    );
    return Objects.requireNonNull(idGenerado.getKey()).intValue();
  }
}
