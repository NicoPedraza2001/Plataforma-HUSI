package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.AsignaturaRequest;
import co.edu.javeriana.husi.dto.response.AsignaturaResponse;

public interface AsignaturaService {
  AsignaturaResponse consultar(Integer id);
  AsignaturaResponse crear(AsignaturaRequest request);
  AsignaturaResponse actualizar(Integer id, AsignaturaRequest request);
  AsignaturaResponse cambiarEstado(Integer id, String estado);
  void eliminarDefinitivamente(Integer id);
}
