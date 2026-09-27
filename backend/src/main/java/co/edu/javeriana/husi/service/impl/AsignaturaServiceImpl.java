package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.AsignaturaRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.AsignaturaMapper;
import co.edu.javeriana.husi.model.entity.Asignatura;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.AsignaturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AsignaturaServiceImpl implements AsignaturaService {

  private final AsignaturaRepository registros;
  private final ServicioRepository servicios;
  private final AsignaturaMapper mapper;

  private NegocioException error(HttpStatus estado, String mensaje) {
    return new NegocioException(estado, mensaje);
  }

  private Asignatura obtener(Integer id, boolean bloquear) {
    return (bloquear ? registros.bloquearPorId(id) : registros.findById(id))
      .filter(e -> !"Eliminado".equals(e.getEstado()))
      .orElseThrow(() ->
        error(HttpStatus.NOT_FOUND, "No se encontró el registro.")
      );
  }

  private void aplicar(Asignatura e, AsignaturaRequest d) {
    if (d.servicioId() == null || d.servicioId() < 1) throw error(
      HttpStatus.BAD_REQUEST,
      "Selecciona un servicio."
    );
    var servicio = servicios
      .bloquearPorId(d.servicioId())
      .orElseThrow(() ->
        error(HttpStatus.BAD_REQUEST, "El servicio no existe.")
      );
    boolean mismo =
      e.getServicio() != null &&
      e.getServicio().getId().equals(servicio.getId());
    if (
      "Eliminado".equals(servicio.getEstado()) ||
      (!mismo && !"Activo".equals(servicio.getEstado()))
    ) throw error(HttpStatus.CONFLICT, "Selecciona un servicio activo.");

    if (
      d.nombre() == null ||
      d.nombre().isBlank() ||
      d.nombre().strip().length() > 200
    ) throw error(
      HttpStatus.BAD_REQUEST,
      "El nombre es obligatorio y admite hasta 200 caracteres."
    );
    if (
      registros.existsByServicioIdAndNombreIgnoreCaseAndIdNot(
        d.servicioId(),
        d.nombre().strip(),
        e.getId() == null ? 0 : e.getId()
      )
    ) throw error(
      HttpStatus.CONFLICT,
      "Ya existe esa asignatura en el servicio. Revisa también los inactivos."
    );
    if (
      e.getId() != null &&
      !e.getServicio().getId().equals(d.servicioId()) &&
      registros.contarReferencias(e.getId()) > 0
    ) throw error(
      HttpStatus.CONFLICT,
      "La asignatura tiene rotaciones: no se puede cambiar su servicio para conservar el historial."
    );
    e.setNombre(d.nombre().strip());

    e.setServicio(servicio);
  }

  public AsignaturaResponse consultar(Integer id) {
    return mapper.response(obtener(id, false));
  }

  @Transactional
  public AsignaturaResponse crear(AsignaturaRequest d) {
    var e = new Asignatura();
    aplicar(e, d);
    return mapper.response(registros.saveAndFlush(e));
  }

  @Transactional
  public AsignaturaResponse actualizar(Integer id, AsignaturaRequest d) {
    var e = obtener(id, true);
    aplicar(e, d);
    return mapper.response(registros.saveAndFlush(e));
  }

  @Transactional
  public AsignaturaResponse cambiarEstado(Integer id, String estado) {
    if (!"Activo".equals(estado) && !"Inactivo".equals(estado)) throw error(
      HttpStatus.BAD_REQUEST,
      "Estado inválido."
    );
    var e = obtener(id, true);
    e.setEstado(estado);
    return mapper.response(registros.saveAndFlush(e));
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    var e = obtener(id, true);
    if (registros.contarReferencias(id) > 0) throw error(
      HttpStatus.CONFLICT,
      "La asignatura tiene rotaciones asociadas. Puedes desactivarla para conservar el historial."
    );
    registros.delete(e);
    registros.flush();
  }
}
