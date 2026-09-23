package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.ServicioRequest;
import co.edu.javeriana.husi.dto.response.ServicioResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.ServicioMapper;
import co.edu.javeriana.husi.model.entity.Servicio;
import co.edu.javeriana.husi.repository.ServicioRepository;
import co.edu.javeriana.husi.service.ServicioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicioServiceImpl implements ServicioService {

  private final ServicioRepository servicios;
  private final ServicioMapper mapper;

  private Servicio obtener(Integer id, boolean bloquear) {
    return (bloquear ? servicios.bloquearPorId(id) : servicios.findById(id))
      .filter(s -> !"Eliminado".equals(s.getEstado()))
      .orElseThrow(() ->
        new NegocioException(
          HttpStatus.NOT_FOUND,
          "No se encontró el servicio."
        )
      );
  }

  private void aplicar(Servicio s, ServicioRequest d) {
    if (
      d.nombre() == null ||
      d.nombre().isBlank() ||
      d.nombre().strip().length() > 150
    ) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "El nombre es obligatorio y admite hasta 150 caracteres."
    );
    if (
      d.cupoSimultaneo() == null ||
      d.cupoMaximoInstalado() == null ||
      d.cupoSimultaneo() < 0 ||
      d.cupoMaximoInstalado() < 0 ||
      d.cupoSimultaneo() > d.cupoMaximoInstalado()
    ) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "Los cupos deben ser enteros no negativos y el simultáneo no puede superar el máximo instalado."
    );
    if (
      servicios.existsByNombreIgnoreCaseAndIdNot(
        d.nombre().strip(),
        s.getId() == null ? 0 : s.getId()
      )
    ) throw new NegocioException(
      HttpStatus.CONFLICT,
      "Ya existe un servicio con ese nombre. Revisa también los inactivos."
    );
    s.setNombre(d.nombre().strip());
    s.setDescripcion(
      d.descripcion() == null || d.descripcion().isBlank()
        ? null
        : d.descripcion().strip()
    );
    s.setCupoSimultaneo(d.cupoSimultaneo());
    s.setCupoMaximoInstalado(d.cupoMaximoInstalado());
    // La base conserva el cupo de las rotaciones existentes; no se recalcula aquí.
  }

  public ServicioResponse consultar(Integer id) {
    return mapper.response(obtener(id, false));
  }

  @Transactional
  public ServicioResponse crear(ServicioRequest d) {
    var s = new Servicio();
    aplicar(s, d);
    return mapper.response(servicios.saveAndFlush(s));
  }

  @Transactional
  public ServicioResponse actualizar(Integer id, ServicioRequest d) {
    var s = obtener(id, true);
    aplicar(s, d);
    return mapper.response(servicios.saveAndFlush(s));
  }

  @Transactional
  public ServicioResponse cambiarEstado(Integer id, String estado) {
    if (
      !"Activo".equals(estado) && !"Inactivo".equals(estado)
    ) throw new NegocioException(HttpStatus.BAD_REQUEST, "Estado inválido.");
    var s = obtener(id, true);
    s.setEstado(estado);
    return mapper.response(servicios.saveAndFlush(s));
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    var s = obtener(id, true);
    if (servicios.contarReferencias(id) > 0) throw new NegocioException(
      HttpStatus.CONFLICT,
      "El servicio tiene asignaturas o coordinadores asociados, incluso inactivos. Puedes desactivarlo para conservar sus relaciones."
    );
    servicios.delete(s);
    servicios.flush();
  }
}
