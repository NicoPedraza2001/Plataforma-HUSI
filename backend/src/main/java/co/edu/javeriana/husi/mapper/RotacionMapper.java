package co.edu.javeriana.husi.mapper;

import co.edu.javeriana.husi.dto.response.RotacionResponse;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Traduce los alias de la proyección SQL al contrato público. */
@Component
public class RotacionMapper {

  public RotacionResponse toResponse(Map<String, Object> fila) {
    return new RotacionResponse(
      ((Number) fila.get("asignaturaId")).intValue(),
      fila.get("inicio").toString(),
      fila.get("fin").toString(),
      (String) fila.get("asignatura"),
      (String) fila.get("servicio"),
      ((Number) fila.get("servicioId")).intValue(),
      (Boolean) fila.get("activa"),
      (String) fila.get("disponibilidad"),
      ((Number) fila.get("cupoDisponible")).intValue(),
      ((Number) fila.get("relaciones")).longValue()
    );
  }
}
