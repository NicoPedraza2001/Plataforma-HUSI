package co.edu.javeriana.husi.service;
import co.edu.javeriana.husi.dto.response.PaginaResponse;
import java.util.*;
public interface ConsultaService {
 PaginaResponse<Map<String,Object>> listar(String dominio,Map<String,String> parametros);
 void validar(String dominio,Map<String,String> parametros);
 Map<String,List<String>> catalogos();
}
