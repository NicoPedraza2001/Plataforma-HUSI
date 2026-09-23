package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.ServicioRequest;
import co.edu.javeriana.husi.dto.response.ServicioResponse;

public interface ServicioService {
  ServicioResponse consultar(Integer id);
  ServicioResponse crear(ServicioRequest request);
  ServicioResponse actualizar(Integer id, ServicioRequest request);
  ServicioResponse cambiarEstado(Integer id, String estado);
  void eliminarDefinitivamente(Integer id);
}
