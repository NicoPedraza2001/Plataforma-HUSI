package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.TrasladoEsperaRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.model.entity.RotacionId;
import java.util.List;

public interface RotacionResidenteService {
  /**
   * Con {@code soloEnEspera} lista solo «Esperando cupo» por orden de llegada; sin él, el resto
   * (asignados y cancelados) por nombre.
   */
  PaginaResponse<ResidenteRotacionResponse> listar(
    RotacionId id,
    int pagina,
    boolean soloEnEspera
  );
  List<CatalogoResponse> candidatos(RotacionId id, String busqueda);
  void asignar(RotacionId id, Integer residenteId);
  /** Solo para la carga inicial desde Excel: admite rotaciones finalizadas. */
  void asignarHistorica(RotacionId id, Integer residenteId);
  void cambiarEstado(RotacionId id, Integer residenteId, String nuevoEstado);
  /** Borra la relación de un residente sin cupo (en espera o cancelado). */
  void eliminar(RotacionId id, Integer residenteId);
  /** Residentes en espera en otras rotaciones del mismo servicio. */
  List<EsperaServicioResponse> listarEsperaDelServicio(RotacionId id);
  void trasladarEspera(RotacionId id, TrasladoEsperaRequest traslado);
}
