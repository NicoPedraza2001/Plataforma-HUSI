package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.CoordinadorRequest;
import co.edu.javeriana.husi.dto.response.CatalogoResponse;
import co.edu.javeriana.husi.dto.response.CoordinadorResponse;
import java.util.List;

public interface CoordinadorService {
  CoordinadorResponse consultar(Integer id);
  CoordinadorResponse crear(CoordinadorRequest request);
  CoordinadorResponse actualizar(Integer id, CoordinadorRequest request);
  CoordinadorResponse cambiarEstado(Integer id, String estado);
  List<CatalogoResponse> usuariosDisponibles();
  void eliminarDefinitivamente(Integer id);
}
