package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.ServicioRequest;
import co.edu.javeriana.husi.dto.response.CatalogoResponse;
import co.edu.javeriana.husi.dto.response.ServicioResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.ServicioMapper;
import co.edu.javeriana.husi.model.entity.Servicio;
import co.edu.javeriana.husi.repository.ServicioRepository;
import co.edu.javeriana.husi.service.ServicioService;
import co.edu.javeriana.husi.util.Calendario;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicioServiceImpl implements ServicioService {

  private final ServicioRepository servicioRepository;
  private final ServicioMapper servicioMapper;

  private Servicio buscarServicio(Integer id, boolean bloquear) {
    return (
      bloquear
        ? servicioRepository.bloquearPorId(id)
        : servicioRepository.findById(id)
    )
      .filter(servicio -> !"Eliminado".equals(servicio.getEstado()))
      .orElseThrow(() ->
        new NegocioException(
          HttpStatus.NOT_FOUND,
          "No se encontró el servicio."
        )
      );
  }

  /** Valida la solicitud y copia sus datos sobre el servicio (nuevo o existente). */
  private void validarYAsignarDatos(Servicio servicio, ServicioRequest request) {
    if (
      request.nombre() == null ||
      request.nombre().isBlank() ||
      request.nombre().strip().length() > 150
    ) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "El nombre es obligatorio y admite hasta 150 caracteres."
    );
    if (
      request.cupoSimultaneo() == null ||
      request.cupoMaximoInstalado() == null ||
      request.cupoSimultaneo() < 0 ||
      request.cupoMaximoInstalado() < 0 ||
      request.cupoSimultaneo() > request.cupoMaximoInstalado()
    ) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "Los cupos deben ser enteros no negativos y el simultáneo no puede superar el máximo instalado."
    );
    if (
      servicioRepository.existsByNombreIgnoreCaseAndIdNot(
        request.nombre().strip(),
        servicio.getId() == null ? 0 : servicio.getId()
      )
    ) throw new NegocioException(
      HttpStatus.CONFLICT,
      "Ya existe un servicio con ese nombre. Revisa también los inactivos."
    );
    servicio.setNombre(request.nombre().strip());
    servicio.setDescripcion(
      request.descripcion() == null || request.descripcion().isBlank()
        ? null
        : request.descripcion().strip()
    );
    servicio.setCupoSimultaneo(request.cupoSimultaneo());
    servicio.setCupoMaximoInstalado(request.cupoMaximoInstalado());
    // Al guardar, trg_servicio_recalcular_cupo recalcula el cupo de sus rotaciones vigentes.
  }

  public List<CatalogoResponse> catalogo(boolean soloActivos) {
    return servicioRepository
      .catalogo(soloActivos)
      .stream()
      .map(servicio -> new CatalogoResponse(servicio.getId(), servicio.getNombre()))
      .toList();
  }

  public ServicioResponse consultar(Integer id) {
    return servicioMapper.toResponse(buscarServicio(id, false));
  }

  @Transactional
  public ServicioResponse crear(ServicioRequest request) {
    var servicio = new Servicio();
    validarYAsignarDatos(servicio, request);
    return servicioMapper.toResponse(servicioRepository.saveAndFlush(servicio));
  }

  @Transactional
  public ServicioResponse actualizar(Integer id, ServicioRequest request) {
    var servicio = buscarServicio(id, true);
    // Antes de asignar los datos: la consulta nativa haría flush y el trigger fallaría primero.
    long cupoMinimoPermitido = servicioRepository.maximoAsignadosPorRotacion(
      id,
      Calendario.hoy()
    );
    if (
      request.cupoSimultaneo() != null &&
      request.cupoSimultaneo() < cupoMinimoPermitido
    ) throw new NegocioException(
      HttpStatus.CONFLICT,
      "No puedes bajar el cupo simultáneo a " +
        request.cupoSimultaneo() +
        ": una rotación vigente de este servicio ya tiene " +
        cupoMinimoPermitido +
        " residentes asignados. El mínimo permitido es " +
        cupoMinimoPermitido +
        "; cancela asignaciones si necesitas reducirlo más."
    );
    validarYAsignarDatos(servicio, request);
    return servicioMapper.toResponse(servicioRepository.saveAndFlush(servicio));
  }

  @Transactional
  public ServicioResponse cambiarEstado(Integer id, String estado) {
    if (
      !"Activo".equals(estado) && !"Inactivo".equals(estado)
    ) throw new NegocioException(HttpStatus.BAD_REQUEST, "Estado inválido.");
    var servicio = buscarServicio(id, true);
    servicio.setEstado(estado);
    return servicioMapper.toResponse(servicioRepository.saveAndFlush(servicio));
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    var servicio = buscarServicio(id, true);
    if (servicioRepository.contarReferencias(id) > 0) throw new NegocioException(
      HttpStatus.CONFLICT,
      "El servicio tiene asignaturas o coordinadores asociados, incluso inactivos. Puedes desactivarlo para conservar sus relaciones."
    );
    servicioRepository.delete(servicio);
    servicioRepository.flush();
  }
}
