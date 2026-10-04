package co.edu.javeriana.husi.repository;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CargaInicialRepository {

  private final JdbcTemplate jdbc;

  /**
   * Lee completas las tablas que intervienen en la carga inicial (nombre de tabla → filas).
   * Con {@code bloquear} usa FOR UPDATE para que nadie las cambie mientras se confirma.
   */
  public Map<String, List<Map<String, Object>>> leerTablas(boolean bloquear) {
    Map<String, List<Map<String, Object>>> tablas = new LinkedHashMap<>();
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
      var filas = jdbc.queryForList(
        "select * from " +
          tabla +
          " order by " +
          orden +
          (bloquear ? " for update" : "")
      );
      // El plan compara el residente por el código y el nombre de sus
      // catálogos; se leen aparte porque FOR UPDATE no admite el LEFT JOIN.
      if (tabla.equals("residente")) filas = jdbc.queryForList(
        "select r.*,td.codigo as tipo_documento,pa.nombre as perfil_academico from residente r join tipo_documento td on td.id_tipo_documento=r.id_tipo_documento left join perfil_academico pa on pa.id_perfil_academico=r.id_perfil_academico order by r.id_residente"
      );
      tablas.put(tabla, filas);
    }
    for (String catalogo : List.of("tipo_documento", "perfil_academico")) tablas.put(
      catalogo,
      jdbc.queryForList(
        "select * from " + catalogo + " order by id_" + catalogo
      )
    );
    return tablas;
  }
}
