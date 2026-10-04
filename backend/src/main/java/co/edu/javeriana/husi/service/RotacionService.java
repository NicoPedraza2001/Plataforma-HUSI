package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.PaginaResponse;
import co.edu.javeriana.husi.dto.response.RotacionResponse;
import co.edu.javeriana.husi.model.entity.RotacionId;
import java.util.*;

public interface RotacionService {
  PaginaResponse<RotacionResponse> listar(Map<String, String> filtros);
  Map<String, Object> catalogos();
  RotacionResponse consultar(RotacionId id);
  RotacionResponse crear(RotacionRequest datos);
  /** Solo para la carga inicial desde Excel: admite rotaciones históricas. */
  RotacionResponse crearHistorica(RotacionRequest datos);
  RotacionResponse editar(RotacionId id, RotacionRequest datos);
  RotacionResponse cambiarEstado(RotacionId id, boolean activa);
  void eliminar(RotacionId id);
  RotacionRequest validarFila(ImportacionRotacionesRequest.Fila fila);
}
