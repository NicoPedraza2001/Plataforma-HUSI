package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.UsuarioRequest;
import co.edu.javeriana.husi.dto.response.UsuarioResponse;

public interface UsuarioService {
  UsuarioResponse consultar(Integer id);
  UsuarioResponse crear(UsuarioRequest request);
  UsuarioResponse actualizar(Integer id, UsuarioRequest request);
  UsuarioResponse cambiarEstado(Integer id, String estado);
  void eliminarDefinitivamente(Integer id);
}
