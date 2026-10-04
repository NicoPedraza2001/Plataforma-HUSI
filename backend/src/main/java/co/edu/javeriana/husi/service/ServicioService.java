package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.ServicioRequest;
import co.edu.javeriana.husi.dto.response.CatalogoResponse;
import co.edu.javeriana.husi.dto.response.ServicioResponse;
import java.util.List;

public interface ServicioService {
  List<CatalogoResponse> catalogo(boolean soloActivos);
  ServicioResponse consultar(Integer id);
  ServicioResponse crear(ServicioRequest request);
  ServicioResponse actualizar(Integer id, ServicioRequest request);
  ServicioResponse cambiarEstado(Integer id, String estado);
  void eliminarDefinitivamente(Integer id);
}
