package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.CoordinadorRequest;
import co.edu.javeriana.husi.dto.response.CoordinadorResponse;

public interface CoordinadorService {
  CoordinadorResponse consultar(Integer id);
  CoordinadorResponse crear(CoordinadorRequest request);
  CoordinadorResponse actualizar(Integer id, CoordinadorRequest request);
  CoordinadorResponse cambiarEstado(Integer id, String estado);
  java.util.List<co.edu.javeriana.husi.dto.response.CatalogoResponse> usuariosDisponibles();
  void eliminarDefinitivamente(Integer id);
}
