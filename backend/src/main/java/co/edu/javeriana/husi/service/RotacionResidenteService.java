package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.TrasladoEsperaRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.model.entity.RotacionId;
import java.util.List;

public interface RotacionResidenteService {
  /**
   * Con {@code espera} lista solo «Esperando cupo» por orden de llegada; sin él, el resto
   * (asignados y cancelados) por nombre.
   */
  PaginaResponse<ResidenteRotacionResponse> listar(
    RotacionId id,
    int pagina,
    boolean espera
  );
  List<CatalogoResponse> candidatos(RotacionId id, String q);
  void asignar(RotacionId id, Integer residente);
  /** Solo para la carga inicial desde Excel: admite rotaciones finalizadas. */
  void asignarHistorica(RotacionId id, Integer residente);
  void estado(RotacionId id, Integer residente, String estado);
  /** Borra la relación de un residente sin cupo (en espera o cancelado). */
  void eliminar(RotacionId id, Integer residente);
  List<EsperaServicioResponse> espera(RotacionId id);
  void trasladarEspera(RotacionId id, TrasladoEsperaRequest d);
}
