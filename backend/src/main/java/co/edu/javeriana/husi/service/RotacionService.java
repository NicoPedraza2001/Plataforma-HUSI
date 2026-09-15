package co.edu.javeriana.husi.service;
import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.PaginaResponse;
import co.edu.javeriana.husi.model.entity.RotacionId;
import java.util.*;
public interface RotacionService {
 PaginaResponse<Map<String,Object>> listar(Map<String,String> filtros);
 Map<String,Object> catalogos();
 Map<String,Object> consultar(RotacionId id);
 Map<String,Object> crear(RotacionRequest datos);
 Map<String,Object> editar(RotacionId id,RotacionRequest datos);
 Map<String,Object> estado(RotacionId id,boolean activa);
 void eliminar(RotacionId id);
 RotacionRequest validarFila(ImportacionRotacionesRequest.Fila fila);
}
