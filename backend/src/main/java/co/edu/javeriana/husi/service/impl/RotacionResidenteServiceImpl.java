package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.TrasladoEsperaRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.FotografiaAuditoriaMapper;
import co.edu.javeriana.husi.mapper.FotografiaRegistro;
import co.edu.javeriana.husi.mapper.RotacionMapper;
import co.edu.javeriana.husi.model.entity.*;
import co.edu.javeriana.husi.model.enums.AccionAuditoria;
import co.edu.javeriana.husi.model.enums.ModuloAuditoria;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.HistorialAuditoriaService;
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

  private static final Set<String> ESTADOS_ASIGNACION = Set.of(
    "Asignado",
    "Cancelado",
    "Esperando cupo"
  );

  private final RotacionRepository rotacionRepository;
  private final RotacionResidenteRepository rotacionResidenteRepository;
  private final ResidenteRepository residenteRepository;
  private final RotacionConsultaRepository rotacionConsultaRepository;
  private final RotacionMapper rotacionMapper;
  private final HistorialAuditoriaService historialAuditoria;
  private final FotografiaAuditoriaMapper fotografias;

  private NegocioException error(HttpStatus estado, String mensaje) {
    return new NegocioException(estado, mensaje);
  }

  private Rotacion buscarRotacion(RotacionId id, boolean bloquear) {
    return (
      bloquear
        ? rotacionRepository.bloquearPorId(id)
        : rotacionRepository.findById(id)
    ).orElseThrow(() ->
      error(HttpStatus.NOT_FOUND, "No se encontró la rotación.")
    );
  }

  private void validarRotacionActiva(Rotacion rotacion) {
    if (
      !rotacion.isActiva() ||
      !rotacionConsultaRepository.asignaturaActiva(
        rotacion.getId().getAsignaturaId()
      )
    ) throw error(
      HttpStatus.CONFLICT,
      "La rotación, su asignatura y su servicio deben estar activos."
    );
  }

  /** Una rotación cuya fecha final ya pasó es de solo lectura. */
  private void validarNoFinalizada(RotacionId id) {
    if (Calendario.finalizada(id.getFin())) throw error(
      HttpStatus.CONFLICT,
      "La rotación finalizó el " +
        Calendario.formato(id.getFin()) +
        "; sus residentes ya no pueden modificarse."
    );
  }

  private void validarResidenteActivo(Integer residenteId) {
    var residente = residenteRepository
      .bloquearPorId(residenteId)
      .orElseThrow(() ->
        error(HttpStatus.BAD_REQUEST, "No se encontró el residente.")
      );
    if (!"Activo".equals(residente.getEstado())) throw error(
      HttpStatus.CONFLICT,
      "El residente debe estar activo."
    );
  }

  private void validarAsignacionExiste(RotacionId id, Integer residenteId) {
    if (!rotacionResidenteRepository.existe(id, residenteId)) throw error(
      HttpStatus.NOT_FOUND,
      "No se encontró la asignación."
    );
  }

  private boolean estaAsignado(RotacionId id, Integer residenteId) {
    return "Asignado".equals(
      rotacionResidenteRepository.consultarEstado(id, residenteId)
    );
  }

  /** Residente, rotación y estado actual de la asignación, tal como los guarda el historial. */
  private FotografiaRegistro fotografiarAsignacion(RotacionId id, Integer residenteId) {
    var residente = residenteRepository
      .findById(residenteId)
      .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No se encontró el residente."));
    var rotacion = rotacionMapper.toResponse(rotacionConsultaRepository.detalle(id));
    return fotografias.deAsignacion(
      residente,
      rotacion,
      rotacionResidenteRepository.consultarEstado(id, residenteId)
    );
  }

  public PaginaResponse<ResidenteRotacionResponse> listar(
    RotacionId id,
    int pagina,
    boolean soloEnEspera
  ) {
    buscarRotacion(id, false);
    if (pagina < 1 || pagina > 1000000) throw error(
      HttpStatus.BAD_REQUEST,
      "Página inválida."
    );
    return rotacionResidenteRepository.listar(id, pagina, soloEnEspera);
  }

  public List<CatalogoResponse> candidatos(RotacionId id, String busqueda) {
    buscarRotacion(id, false);
    if (busqueda.length() > 200) throw error(
      HttpStatus.BAD_REQUEST,
      "La búsqueda admite 200 caracteres."
    );
    return rotacionResidenteRepository.candidatos(id, busqueda.strip());
  }

  @Transactional
  public void asignar(RotacionId id, Integer residenteId) {
    validarNoFinalizada(id);
    asignarHistorica(id, residenteId);
  }

  @Transactional
  public void asignarHistorica(RotacionId id, Integer residenteId) {
    var rotacion = buscarRotacion(id, true);
    validarRotacionActiva(rotacion);
    validarResidenteActivo(residenteId);
    if (rotacionResidenteRepository.existe(id, residenteId)) throw error(
      HttpStatus.CONFLICT,
      "El residente ya está relacionado; revisa también las asignaciones canceladas."
    );
    rotacionResidenteRepository.insertar(id, residenteId);
    // El estado (Asignado o Esperando cupo) lo decide la base de datos según el cupo.
    historialAuditoria.registrar(
      AccionAuditoria.ASIGNO,
      ModuloAuditoria.ASIGNACIONES,
      null,
      fotografiarAsignacion(id, residenteId)
    );
  }

  @Transactional
  public void cambiarEstado(
    RotacionId id,
    Integer residenteId,
    String nuevoEstado
  ) {
    var rotacion = buscarRotacion(id, true);
    validarNoFinalizada(id);
    if (!ESTADOS_ASIGNACION.contains(nuevoEstado)) throw error(
      HttpStatus.BAD_REQUEST,
      "Estado de asignación inválido."
    );
    validarAsignacionExiste(id, residenteId);
    if ("Esperando cupo".equals(nuevoEstado)) {
      // Quitar el cupo: pasa al final de la espera y su cupo lo toma el siguiente.
      if (!estaAsignado(id, residenteId)) throw error(
        HttpStatus.CONFLICT,
        "Solo se puede quitar el cupo a un residente asignado."
      );
      validarRotacionActiva(rotacion);
    }
    if ("Asignado".equals(nuevoEstado)) {
      validarRotacionActiva(rotacion);
      validarResidenteActivo(residenteId);
      if (
        !estaAsignado(id, residenteId) &&
        rotacionResidenteRepository.cupoDisponible(id) == 0
      ) throw error(
        HttpStatus.CONFLICT,
        "No hay cupos disponibles. El residente permanece en su estado actual."
      );
    }
    FotografiaRegistro antes = fotografiarAsignacion(id, residenteId);
    rotacionResidenteRepository.cambiarEstado(id, residenteId, nuevoEstado);
    historialAuditoria.registrar(
      AccionAuditoria.CAMBIO_ESTADO,
      ModuloAuditoria.ASIGNACIONES,
      antes,
      fotografiarAsignacion(id, residenteId)
    );
  }

  @Transactional
  public void eliminar(RotacionId id, Integer residenteId) {
    buscarRotacion(id, true);
    validarNoFinalizada(id);
    validarAsignacionExiste(id, residenteId);
    if (estaAsignado(id, residenteId)) throw error(
      HttpStatus.CONFLICT,
      "El residente tiene cupo asignado. Quítale el cupo antes de eliminarlo."
    );
    FotografiaRegistro antes = fotografiarAsignacion(id, residenteId);
    rotacionResidenteRepository.eliminar(id, residenteId);
    historialAuditoria.registrar(
      AccionAuditoria.ELIMINO,
      ModuloAuditoria.ASIGNACIONES,
      antes,
      null
    );
  }

  public List<EsperaServicioResponse> listarEsperaDelServicio(RotacionId id) {
    buscarRotacion(id, false);
    return rotacionResidenteRepository.listarEsperaDelServicio(id);
  }

  @Transactional
  public void trasladarEspera(RotacionId id, TrasladoEsperaRequest traslado) {
    var rotacionDestino = buscarRotacion(id, true);
    validarNoFinalizada(id);
    validarRotacionActiva(rotacionDestino);
    int cantidadSolicitada = traslado.residentes().size();
    long residentesDistintos = traslado
      .residentes()
      .stream()
      .map(TrasladoEsperaRequest.Origen::residenteId)
      .distinct()
      .count();
    if (residentesDistintos < cantidadSolicitada) throw error(
      HttpStatus.BAD_REQUEST,
      "Un residente solo puede trasladarse una vez a la misma rotación."
    );
    int cupos = rotacionResidenteRepository.cupoDisponible(id);
    if (cantidadSolicitada > cupos) throw error(
      HttpStatus.CONFLICT,
      cupos == 0
        ? "La rotación no tiene cupos disponibles."
        : "La rotación solo tiene " +
          (cupos == 1 ? "1 cupo disponible" : cupos + " cupos disponibles") +
          "; selecciona como máximo " +
          cupos +
          (cupos == 1 ? " residente." : " residentes.")
    );
    for (var origen : traslado.residentes()) {
      validarResidenteActivo(origen.residenteId());
      if (rotacionResidenteRepository.existe(id, origen.residenteId())) throw error(
        HttpStatus.CONFLICT,
        "Uno de los residentes ya está relacionado con esta rotación."
      );
      var rotacionOrigen = new RotacionId(
        origen.asignaturaId(),
        origen.inicio(),
        origen.fin()
      );
      // Si no está en la rotación de origen, el traslado de abajo falla y no se registra nada.
      FotografiaRegistro antes = null;
      if (
        rotacionResidenteRepository.existe(rotacionOrigen, origen.residenteId())
      ) antes = fotografiarAsignacion(rotacionOrigen, origen.residenteId());
      int filasActualizadas = rotacionResidenteRepository.trasladar(
        rotacionOrigen,
        id,
        origen.residenteId()
      );
      if (filasActualizadas == 0) throw error(
        HttpStatus.CONFLICT,
        "Uno de los residentes ya no está en espera en esa rotación o pertenece a otro servicio. Actualiza la lista e inténtalo de nuevo."
      );
      historialAuditoria.registrar(
        AccionAuditoria.TRASLADO,
        ModuloAuditoria.ASIGNACIONES,
        antes,
        fotografiarAsignacion(id, origen.residenteId())
      );
    }
  }
}
