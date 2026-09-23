package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.ResidenteRequest;
import co.edu.javeriana.husi.dto.response.ResidenteResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.ResidenteMapper;
import co.edu.javeriana.husi.model.entity.Residente;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.ResidenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResidenteServiceImpl implements ResidenteService {

  private final ResidenteRepository residentes;
  private final ProgramaRepository programas;
  private final ResidenteMapper mapper;

  private Residente obtener(Integer id) {
    return residentes
      .bloquearPorId(id)
      .filter(e -> !"Eliminado".equals(e.getEstado()))
      .orElseThrow(() ->
        new NegocioException(
          HttpStatus.NOT_FOUND,
          "No se encontró el residente."
        )
      );
  }

  private void aplicar(Residente e, ResidenteRequest d) {
    var p = programas
      .bloquearPorId(d.programaId())
      .orElseThrow(() ->
        new NegocioException(HttpStatus.BAD_REQUEST, "El programa no existe.")
      );
    if (
      !d.programaId().equals(e.getProgramaId()) &&
      (!"Activo".equals(p.getEstado()) ||
        !"Activo".equals(p.getUniversidad().getEstado()))
    ) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "Selecciona un programa y una universidad activos."
    );
    if (
      residentes.existsByNumeroDocumentoIgnoreCaseAndIdNot(
        d.numeroDocumento().strip(),
        e.getId() == null ? 0 : e.getId()
      )
    ) throw new NegocioException(
      HttpStatus.CONFLICT,
      "Ya existe un residente con ese documento."
    );
    e.setNombre(d.nombre().strip());
    e.setTipoDocumento(d.tipoDocumento().strip());
    e.setNumeroDocumento(d.numeroDocumento().strip());
    e.setPerfilAcademico(
      d.perfilAcademico() == null ? null : d.perfilAcademico().strip()
    );
    e.setProgramaId(d.programaId());
  }

  @Transactional
  public ResidenteResponse consultar(Integer id) {
    return mapper.response(obtener(id));
  }

  @Transactional
  public ResidenteResponse crear(ResidenteRequest d) {
    var e = new Residente();
    aplicar(e, d);
    return mapper.response(residentes.saveAndFlush(e));
  }

  @Transactional
  public ResidenteResponse actualizar(Integer id, ResidenteRequest d) {
    var e = obtener(id);
    aplicar(e, d);
    return mapper.response(residentes.saveAndFlush(e));
  }

  @Transactional
  public ResidenteResponse cambiarEstado(Integer id, String estado) {
    if (
      !"Activo".equals(estado) && !"Inactivo".equals(estado)
    ) throw new NegocioException(HttpStatus.BAD_REQUEST, "Estado inválido.");
    var e = obtener(id);
    e.setEstado(estado);
    return mapper.response(residentes.saveAndFlush(e));
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    var e = obtener(id);
    if (residentes.contarReferencias(id) > 0) throw new NegocioException(
      HttpStatus.CONFLICT,
      "El residente tiene rotaciones asociadas. Puedes desactivarlo para conservar su historial."
    );
    residentes.delete(e);
    residentes.flush();
  }
}
