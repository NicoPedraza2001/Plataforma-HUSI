package co.edu.javeriana.husi.service;
import co.edu.javeriana.husi.dto.request.UniversidadRequest;
import co.edu.javeriana.husi.dto.response.*;
import java.util.*;
public interface UniversidadService {
 List<CatalogoResponse> catalogo();
 UniversidadResponse consultar(Integer id);
 UniversidadResponse crear(UniversidadRequest request);
 UniversidadResponse actualizar(Integer id,UniversidadRequest request);
 void eliminarDefinitivamente(Integer id);
 UniversidadResponse cambiarEstado(Integer id,String estado);
 PaginaResponse<Map<String,Object>> programas(Integer id,Map<String,String> filtros);
}
