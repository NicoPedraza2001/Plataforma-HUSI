package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.model.entity.*;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.RotacionResidenteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RotacionResidenteServiceImpl implements RotacionResidenteService {

  private final RotacionRepository rotaciones;
  private final RotacionResidenteRepository asignaciones;
  private final ResidenteRepository residentes;
  private final RotacionConsultaRepository consultas;

  private NegocioException error(HttpStatus status, String mensaje) {
    return new NegocioException(status, mensaje);
  }

  private Rotacion obtener(RotacionId id, boolean lock) {
    return (
      lock ? rotaciones.bloquear(id) : rotaciones.findById(id)
    ).orElseThrow(() ->
      error(HttpStatus.NOT_FOUND, "No se encontró la rotación.")
    );
  }

  private void validarActivo(Rotacion r) {
    if (
      !r.isActiva() || !consultas.asignaturaActiva(r.getId().getAsignaturaId())
    ) throw error(
      HttpStatus.CONFLICT,
      "La rotación, su asignatura y su servicio deben estar activos."
    );
  }

  private void residenteActivo(Integer id) {
    var r = residentes
      .bloquearPorId(id)
      .orElseThrow(() ->
        error(HttpStatus.BAD_REQUEST, "No se encontró el residente.")
      );
    if (!"Activo".equals(r.getEstado())) throw error(
      HttpStatus.CONFLICT,
      "El residente debe estar activo."
    );
  }

  public PaginaResponse<ResidenteRotacionResponse> listar(
    RotacionId id,
    int pagina
  ) {
    obtener(id, false);
    if (pagina < 1 || pagina > 1000000) throw error(
      HttpStatus.BAD_REQUEST,
      "Página inválida."
    );
    return asignaciones.listar(id, pagina);
  }

  public List<CatalogoResponse> candidatos(RotacionId id, String q) {
    obtener(id, false);
    if (q.length() > 200) throw error(
      HttpStatus.BAD_REQUEST,
      "La búsqueda admite 200 caracteres."
    );
    return asignaciones.candidatos(id, q.strip());
  }

  @Transactional
  public void asignar(RotacionId id, Integer residente) {
    var r = obtener(id, true);
    validarActivo(r);
    residenteActivo(residente);
    if (asignaciones.existe(id, residente)) throw error(
      HttpStatus.CONFLICT,
      "El residente ya está relacionado; revisa también las asignaciones canceladas."
    );
    asignaciones.insertar(id, residente);
  }

  @Transactional
  public void estado(RotacionId id, Integer residente, String estado) {
    var r = obtener(id, true);
    if (!"Asignado".equals(estado) && !"Cancelado".equals(estado)) throw error(
      HttpStatus.BAD_REQUEST,
      "Estado de asignación inválido."
    );
    if (!asignaciones.existe(id, residente)) throw error(
      HttpStatus.NOT_FOUND,
      "No se encontró la asignación."
    );
    if ("Asignado".equals(estado)) {
      validarActivo(r);
      residenteActivo(residente);
      if (
        !"Asignado".equals(asignaciones.estado(id, residente)) &&
        asignaciones.cupos(id) == 0
      ) throw error(
        HttpStatus.CONFLICT,
        "No hay cupos disponibles. El residente permanece en su estado actual."
      );
    }
    asignaciones.cambiarEstado(id, residente, estado);
  }
}
