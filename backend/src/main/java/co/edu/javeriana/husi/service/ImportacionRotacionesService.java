package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.ImportacionRotacionesRequest;
import java.util.Map;

/** Validación y guardado por fila, sin una transacción global para todo el archivo. */
public interface ImportacionRotacionesService {
  Map<String, Object> procesar(
    ImportacionRotacionesRequest request,
    boolean guardar
  );
}
