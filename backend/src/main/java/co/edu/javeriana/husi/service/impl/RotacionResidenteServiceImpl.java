package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.TrasladoEsperaRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.model.entity.*;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.RotacionResidenteService;
import co.edu.javeriana.husi.util.Calendario;
import java.util.List;
import java.util.Set;
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

  /** Una rotación cuya fecha final ya pasó es de solo lectura. */
  private void noFinalizada(RotacionId id) {
    if (Calendario.finalizada(id.getFin())) throw error(
      HttpStatus.CONFLICT,
      "La rotación finalizó el " +
        Calendario.formato(id.getFin()) +
        "; sus residentes ya no pueden modificarse."
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
    int pagina,
    boolean espera
  ) {
    obtener(id, false);
    if (pagina < 1 || pagina > 1000000) throw error(
      HttpStatus.BAD_REQUEST,
      "Página inválida."
    );
    return asignaciones.listar(id, pagina, espera);
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
    noFinalizada(id);
    asignarHistorica(id, residente);
  }

  @Transactional
  public void asignarHistorica(RotacionId id, Integer residente) {
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
    noFinalizada(id);
    if (
      !Set.of("Asignado", "Cancelado", "Esperando cupo").contains(estado)
    ) throw error(HttpStatus.BAD_REQUEST, "Estado de asignación inválido.");
    if (!asignaciones.existe(id, residente)) throw error(
      HttpStatus.NOT_FOUND,
      "No se encontró la asignación."
    );
    if ("Esperando cupo".equals(estado)) {
      // Quitar el cupo: pasa al final de la espera y su cupo lo toma el siguiente.
      if (!"Asignado".equals(asignaciones.estado(id, residente))) throw error(
        HttpStatus.CONFLICT,
        "Solo se puede quitar el cupo a un residente asignado."
      );
      validarActivo(r);
    }
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

  @Transactional
  public void eliminar(RotacionId id, Integer residente) {
    obtener(id, true);
    noFinalizada(id);
    if (!asignaciones.existe(id, residente)) throw error(
      HttpStatus.NOT_FOUND,
      "No se encontró la asignación."
    );
    if ("Asignado".equals(asignaciones.estado(id, residente))) throw error(
      HttpStatus.CONFLICT,
      "El residente tiene cupo asignado. Quítale el cupo antes de eliminarlo."
    );
    asignaciones.eliminar(id, residente);
  }

  public List<EsperaServicioResponse> espera(RotacionId id) {
    obtener(id, false);
    return asignaciones.espera(id);
  }

  @Transactional
  public void trasladarEspera(RotacionId id, TrasladoEsperaRequest d) {
    var r = obtener(id, true);
    noFinalizada(id);
    validarActivo(r);
    var residentesUnicos = d
      .residentes()
      .stream()
      .map(TrasladoEsperaRequest.Origen::residenteId)
      .distinct()
      .count();
    if (residentesUnicos < d.residentes().size()) throw error(
      HttpStatus.BAD_REQUEST,
      "Un residente solo puede trasladarse una vez a la misma rotación."
    );
    int cupos = asignaciones.cupos(id);
    if (d.residentes().size() > cupos) throw error(
      HttpStatus.CONFLICT,
      cupos == 0
        ? "La rotación no tiene cupos disponibles."
        : "La rotación solo tiene " +
          (cupos == 1 ? "1 cupo disponible" : cupos + " cupos disponibles") +
          "; selecciona como máximo " +
          cupos +
          (cupos == 1 ? " residente." : " residentes.")
    );
    for (var o : d.residentes()) {
      residenteActivo(o.residenteId());
      if (asignaciones.existe(id, o.residenteId())) throw error(
        HttpStatus.CONFLICT,
        "Uno de los residentes ya está relacionado con esta rotación."
      );
      if (
        asignaciones.trasladar(
          new RotacionId(o.asignaturaId(), o.inicio(), o.fin()),
          id,
          o.residenteId()
        ) ==
        0
      ) throw error(
        HttpStatus.CONFLICT,
        "Uno de los residentes ya no está en espera en esa rotación o pertenece a otro servicio. Actualiza la lista e inténtalo de nuevo."
      );
    }
  }
}
