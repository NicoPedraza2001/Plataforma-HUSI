package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.model.entity.RotacionId;
import java.util.List;

public interface RotacionResidenteService {
  PaginaResponse<ResidenteRotacionResponse> listar(RotacionId id, int pagina);
  List<CatalogoResponse> candidatos(RotacionId id, String q);
  void asignar(RotacionId id, Integer residente);
  void estado(RotacionId id, Integer residente, String estado);
}
