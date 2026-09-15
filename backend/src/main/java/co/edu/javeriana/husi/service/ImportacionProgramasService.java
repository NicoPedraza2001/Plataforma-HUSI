package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.ImportacionProgramasRequest;
import co.edu.javeriana.husi.dto.response.ImportacionProgramasResponse;

public interface ImportacionProgramasService {
  ImportacionProgramasResponse procesar(
    ImportacionProgramasRequest request,
    boolean guardar
  );
}
