package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.ResidenteRequest;
import co.edu.javeriana.husi.dto.response.ResidenteResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.FotografiaAuditoriaMapper;
import co.edu.javeriana.husi.mapper.FotografiaRegistro;
import co.edu.javeriana.husi.mapper.ResidenteMapper;
import co.edu.javeriana.husi.model.entity.Residente;
import co.edu.javeriana.husi.model.enums.AccionAuditoria;
import co.edu.javeriana.husi.model.enums.ModuloAuditoria;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.HistorialAuditoriaService;
import co.edu.javeriana.husi.service.ResidenteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResidenteServiceImpl implements ResidenteService {

  private final ResidenteRepository residenteRepository;
  private final ProgramaRepository programaRepository;
  private final TipoDocumentoRepository tipoDocumentoRepository;
  private final PerfilAcademicoRepository perfilAcademicoRepository;
  private final ResidenteMapper residenteMapper;
  private final HistorialAuditoriaService historialAuditoria;
  private final FotografiaAuditoriaMapper fotografias;

  private Residente buscarResidenteBloqueado(Integer id) {
    return residenteRepository
      .bloquearPorId(id)
      .filter(residente -> !"Eliminado".equals(residente.getEstado()))
      .orElseThrow(() ->
        new NegocioException(
          HttpStatus.NOT_FOUND,
          "No se encontró el residente."
        )
      );
  }

  /** Valida la solicitud y copia sus datos sobre el residente (nuevo o existente). */
  private void validarYAsignarDatos(
    Residente residente,
    ResidenteRequest request
  ) {
    var programa = programaRepository
      .bloquearPorId(request.programaId())
      .orElseThrow(() ->
        new NegocioException(HttpStatus.BAD_REQUEST, "El programa no existe.")
      );
    // Un programa inactivo solo se acepta si el residente ya pertenecía a él.
    if (
      !request.programaId().equals(residente.getProgramaId()) &&
      (!"Activo".equals(programa.getEstado()) ||
        !"Activo".equals(programa.getUniversidad().getEstado()))
    ) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "Selecciona un programa y una universidad activos."
    );
    if (
      residenteRepository.existsByNumeroDocumentoIgnoreCaseAndIdNot(
        request.numeroDocumento().strip(),
        residente.getId() == null ? 0 : residente.getId()
      )
    ) throw new NegocioException(
      HttpStatus.CONFLICT,
      "Ya existe un residente con ese documento."
    );
    var tipoDocumento = tipoDocumentoRepository
      .findByCodigoIgnoreCase(request.tipoDocumento().strip())
      .orElseThrow(() ->
        new NegocioException(
          HttpStatus.BAD_REQUEST,
          "El tipo de documento no existe."
        )
      );
    String nombrePerfil = request.perfilAcademico() == null
      ? ""
      : request.perfilAcademico().strip();
    var perfilAcademico = nombrePerfil.isEmpty()
      ? null
      : perfilAcademicoRepository
        .findByNombreIgnoreCase(nombrePerfil)
        .orElseThrow(() ->
          new NegocioException(
            HttpStatus.BAD_REQUEST,
            "El perfil académico no existe."
          )
        );
    residente.setNombre(request.nombre().strip());
    residente.setTipoDocumento(tipoDocumento);
    residente.setNumeroDocumento(request.numeroDocumento().strip());
    residente.setPerfilAcademico(perfilAcademico);
    residente.setProgramaId(request.programaId());
  }

  @Transactional
  public ResidenteResponse consultar(Integer id) {
    return residenteMapper.toResponse(buscarResidenteBloqueado(id));
  }

  public List<ResidenteResponse> buscarPorDocumentos(List<String> documentos) {
    var documentosNormalizados = documentos
      .stream()
      .map(documento -> documento.strip().toLowerCase())
      .distinct()
      .toList();
    if (documentosNormalizados.isEmpty()) return List.of();
    return residenteRepository
      .buscarPorDocumentos(documentosNormalizados)
      .stream()
      .map(residenteMapper::toResponse)
      .toList();
  }

  @Transactional
  public ResidenteResponse crear(ResidenteRequest request) {
    var residente = new Residente();
    validarYAsignarDatos(residente, request);
    var residenteCreado = residenteRepository.saveAndFlush(residente);
    historialAuditoria.registrar(
      AccionAuditoria.CREO,
      ModuloAuditoria.RESIDENTES,
      null,
      fotografias.deResidente(residenteCreado)
    );
    return residenteMapper.toResponse(residenteCreado);
  }

  @Transactional
  public ResidenteResponse actualizar(Integer id, ResidenteRequest request) {
    var residente = buscarResidenteBloqueado(id);
    FotografiaRegistro antes = fotografias.deResidente(residente);
    validarYAsignarDatos(residente, request);
    var residenteActualizado = residenteRepository.saveAndFlush(residente);
    historialAuditoria.registrar(
      AccionAuditoria.ACTUALIZO,
      ModuloAuditoria.RESIDENTES,
      antes,
      fotografias.deResidente(residenteActualizado)
    );
    return residenteMapper.toResponse(residenteActualizado);
  }

  @Transactional
  public ResidenteResponse cambiarEstado(Integer id, String estado) {
    if (
      !"Activo".equals(estado) && !"Inactivo".equals(estado)
    ) throw new NegocioException(HttpStatus.BAD_REQUEST, "Estado inválido.");
    var residente = buscarResidenteBloqueado(id);
    FotografiaRegistro antes = fotografias.deResidente(residente);
    residente.setEstado(estado);
    var residenteActualizado = residenteRepository.saveAndFlush(residente);
    historialAuditoria.registrar(
      AccionAuditoria.segunEstado(estado),
      ModuloAuditoria.RESIDENTES,
      antes,
      fotografias.deResidente(residenteActualizado)
    );
    return residenteMapper.toResponse(residenteActualizado);
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    var residente = buscarResidenteBloqueado(id);
    if (residenteRepository.contarReferencias(id) > 0) throw new NegocioException(
      HttpStatus.CONFLICT,
      "El residente tiene rotaciones asociadas. Puedes desactivarlo para conservar su historial."
    );
    FotografiaRegistro antes = fotografias.deResidente(residente);
    residenteRepository.delete(residente);
    residenteRepository.flush();
    historialAuditoria.registrar(
      AccionAuditoria.ELIMINO,
      ModuloAuditoria.RESIDENTES,
      antes,
      null
    );
  }
}
