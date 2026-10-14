package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.AsignaturaRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.AsignaturaMapper;
import co.edu.javeriana.husi.mapper.FotografiaAuditoriaMapper;
import co.edu.javeriana.husi.mapper.FotografiaRegistro;
import co.edu.javeriana.husi.model.entity.Asignatura;
import co.edu.javeriana.husi.model.enums.AccionAuditoria;
import co.edu.javeriana.husi.model.enums.ModuloAuditoria;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.AsignaturaService;
import co.edu.javeriana.husi.service.HistorialAuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AsignaturaServiceImpl implements AsignaturaService {

  private final AsignaturaRepository asignaturaRepository;
  private final ServicioRepository servicioRepository;
  private final AsignaturaMapper asignaturaMapper;
  private final HistorialAuditoriaService historialAuditoria;
  private final FotografiaAuditoriaMapper fotografias;

  private NegocioException error(HttpStatus estado, String mensaje) {
    return new NegocioException(estado, mensaje);
  }

  private Asignatura buscarAsignatura(Integer id, boolean bloquear) {
    return (
      bloquear
        ? asignaturaRepository.bloquearPorId(id)
        : asignaturaRepository.findById(id)
    )
      .filter(asignatura -> !"Eliminado".equals(asignatura.getEstado()))
      .orElseThrow(() ->
        error(HttpStatus.NOT_FOUND, "No se encontró el registro.")
      );
  }

  /** Valida la solicitud y copia nombre y servicio sobre la asignatura (nueva o existente). */
  private void validarYAsignarDatos(
    Asignatura asignatura,
    AsignaturaRequest request
  ) {
    if (request.servicioId() == null || request.servicioId() < 1) throw error(
      HttpStatus.BAD_REQUEST,
      "Selecciona un servicio."
    );
    var servicio = servicioRepository
      .bloquearPorId(request.servicioId())
      .orElseThrow(() ->
        error(HttpStatus.BAD_REQUEST, "El servicio no existe.")
      );
    // Un servicio inactivo solo se acepta si la asignatura ya pertenecía a él.
    boolean conservaServicio =
      asignatura.getServicio() != null &&
      asignatura.getServicio().getId().equals(servicio.getId());
    if (
      "Eliminado".equals(servicio.getEstado()) ||
      (!conservaServicio && !"Activo".equals(servicio.getEstado()))
    ) throw error(HttpStatus.CONFLICT, "Selecciona un servicio activo.");

    if (
      request.nombre() == null ||
      request.nombre().isBlank() ||
      request.nombre().strip().length() > 200
    ) throw error(
      HttpStatus.BAD_REQUEST,
      "El nombre es obligatorio y admite hasta 200 caracteres."
    );
    if (
      asignaturaRepository.existsByServicioIdAndNombreIgnoreCaseAndIdNot(
        request.servicioId(),
        request.nombre().strip(),
        asignatura.getId() == null ? 0 : asignatura.getId()
      )
    ) throw error(
      HttpStatus.CONFLICT,
      "Ya existe esa asignatura en el servicio. Revisa también los inactivos."
    );
    if (
      asignatura.getId() != null &&
      !asignatura.getServicio().getId().equals(request.servicioId()) &&
      asignaturaRepository.contarReferencias(asignatura.getId()) > 0
    ) throw error(
      HttpStatus.CONFLICT,
      "La asignatura tiene rotaciones: no se puede cambiar su servicio para conservar el historial."
    );
    asignatura.setNombre(request.nombre().strip());
    asignatura.setServicio(servicio);
  }

  public java.util.List<AsignaturaCatalogoResponse> catalogo() {
    return asignaturaRepository
      .catalogo()
      .stream()
      .map(asignatura ->
        new AsignaturaCatalogoResponse(
          asignatura.getId(),
          asignatura.getNombre(),
          asignatura.getServicio().getNombre()
        )
      )
      .toList();
  }

  public AsignaturaResponse consultar(Integer id) {
    return asignaturaMapper.toResponse(buscarAsignatura(id, false));
  }

  @Transactional
  public AsignaturaResponse crear(AsignaturaRequest request) {
    var asignatura = new Asignatura();
    validarYAsignarDatos(asignatura, request);
    var asignaturaCreada = asignaturaRepository.saveAndFlush(asignatura);
    historialAuditoria.registrar(
      AccionAuditoria.CREO,
      ModuloAuditoria.ASIGNATURAS,
      null,
      fotografias.deAsignatura(asignaturaCreada)
    );
    return asignaturaMapper.toResponse(asignaturaCreada);
  }

  @Transactional
  public AsignaturaResponse actualizar(Integer id, AsignaturaRequest request) {
    var asignatura = buscarAsignatura(id, true);
    FotografiaRegistro antes = fotografias.deAsignatura(asignatura);
    validarYAsignarDatos(asignatura, request);
    var asignaturaActualizada = asignaturaRepository.saveAndFlush(asignatura);
    historialAuditoria.registrar(
      AccionAuditoria.ACTUALIZO,
      ModuloAuditoria.ASIGNATURAS,
      antes,
      fotografias.deAsignatura(asignaturaActualizada)
    );
    return asignaturaMapper.toResponse(asignaturaActualizada);
  }

  @Transactional
  public AsignaturaResponse cambiarEstado(Integer id, String estado) {
    if (!"Activo".equals(estado) && !"Inactivo".equals(estado)) throw error(
      HttpStatus.BAD_REQUEST,
      "Estado inválido."
    );
    var asignatura = buscarAsignatura(id, true);
    FotografiaRegistro antes = fotografias.deAsignatura(asignatura);
    asignatura.setEstado(estado);
    var asignaturaActualizada = asignaturaRepository.saveAndFlush(asignatura);
    historialAuditoria.registrar(
      AccionAuditoria.segunEstado(estado),
      ModuloAuditoria.ASIGNATURAS,
      antes,
      fotografias.deAsignatura(asignaturaActualizada)
    );
    return asignaturaMapper.toResponse(asignaturaActualizada);
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    var asignatura = buscarAsignatura(id, true);
    if (asignaturaRepository.contarReferencias(id) > 0) throw error(
      HttpStatus.CONFLICT,
      "La asignatura tiene rotaciones asociadas. Puedes desactivarla para conservar el historial."
    );
    FotografiaRegistro antes = fotografias.deAsignatura(asignatura);
    asignaturaRepository.delete(asignatura);
    asignaturaRepository.flush();
    historialAuditoria.registrar(
      AccionAuditoria.ELIMINO,
      ModuloAuditoria.ASIGNATURAS,
      antes,
      null
    );
  }
}
