package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.ResidenteRequest;
import co.edu.javeriana.husi.dto.response.ResidenteResponse;
import java.util.List;

public interface ResidenteService {
  ResidenteResponse consultar(Integer id);
  List<ResidenteResponse> buscarPorDocumentos(List<String> documentos);
  ResidenteResponse crear(ResidenteRequest request);
  ResidenteResponse actualizar(Integer id, ResidenteRequest request);
  ResidenteResponse cambiarEstado(Integer id, String estado);
  void eliminarDefinitivamente(Integer id);
}
