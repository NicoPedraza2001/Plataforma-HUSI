package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.HistorialDetalleResponse;
import co.edu.javeriana.husi.model.entity.HistorialAuditoria;
import co.edu.javeriana.husi.model.entity.Usuario;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Convierte las fotografías en el JSON de las columnas datos_* y la fila en su respuesta. */
@Component
@RequiredArgsConstructor
public class HistorialAuditoriaMapper {

  private static final TypeReference<LinkedHashMap<String, Object>> TIPO_DATOS =
    new TypeReference<>() {};

  private final JsonMapper jsonMapper;

  public String aJson(Map<String, Object> datos) {
    return datos == null ? null : jsonMapper.writeValueAsString(datos);
  }

  /** Conserva el orden de los campos con que se guardaron. */
  public LinkedHashMap<String, Object> leerJson(String json) {
    return json == null || json.isBlank() ? null : jsonMapper.readValue(json, TIPO_DATOS);
  }

  public HistorialDetalleResponse toResponse(HistorialAuditoria historial) {
    Usuario usuario = historial.getUsuario();
    return new HistorialDetalleResponse(
      historial.getId(),
      historial.getFecha(),
      usuario.getNombre() + " " + usuario.getApellido(),
      usuario.getCorreo(),
      historial.getModulo(),
      historial.getAccion(),
      historial.getRegistro(),
      historial.getDetalle(),
      leerJson(historial.getDatosAnteriores()),
      leerJson(historial.getDatosNuevos()),
      historial.getLoteImportacion() != null
    );
  }
}
