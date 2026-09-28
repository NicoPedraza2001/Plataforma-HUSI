package co.edu.javeriana.husi.repository;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CargaInicialRepository {

  private final JdbcTemplate jdbc;

  public Map<String, List<Map<String, Object>>> obtener(boolean bloquear) {
    Map<String, List<Map<String, Object>>> datos = new LinkedHashMap<>();
    for (String tabla : List.of(
      "universidad",
      "programa",
      "residente",
      "servicio",
      "asignatura",
      "rotacion",
      "rotacion_residente"
    )) {
      String orden = switch (tabla) {
        case "rotacion" -> "id_asignatura,fecha_inicio,fecha_fin";
        case "rotacion_residente" -> "id_residente,id_asignatura,fecha_inicio,fecha_fin";
        default -> "id_" + tabla;
      };
      datos.put(
        tabla,
        jdbc.queryForList(
          "select * from " +
            tabla +
            " order by " +
            orden +
            (bloquear ? " for update" : "")
        )
      );
    }
    return datos;
  }
}
