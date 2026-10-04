package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.CoordinadorRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.CoordinadorMapper;
import co.edu.javeriana.husi.model.entity.Coordinador;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.CoordinadorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoordinadorServiceImpl implements CoordinadorService {

  private final CoordinadorRepository coordinadorRepository;
  private final ServicioRepository servicioRepository;
  private final CoordinadorMapper coordinadorMapper;
  private final UsuarioRepository usuarioRepository;

  private NegocioException error(HttpStatus estado, String mensaje) {
    return new NegocioException(estado, mensaje);
  }

  private Coordinador buscarCoordinador(Integer id, boolean bloquear) {
    return (
      bloquear
        ? coordinadorRepository.bloquearPorId(id)
        : coordinadorRepository.findById(id)
    )
      .filter(coordinador -> !"Eliminado".equals(coordinador.getEstado()))
      .orElseThrow(() ->
        error(HttpStatus.NOT_FOUND, "No se encontró el registro.")
      );
  }

  /** Valida la solicitud y copia usuario y servicio sobre el coordinador (nuevo o existente). */
  private void validarYAsignarDatos(
    Coordinador coordinador,
    CoordinadorRequest request
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
    // Un servicio o usuario inactivo solo se acepta si ya estaba asignado.
    boolean conservaServicio =
      coordinador.getServicio() != null &&
      coordinador.getServicio().getId().equals(servicio.getId());
    if (
      "Eliminado".equals(servicio.getEstado()) ||
      (!conservaServicio && !"Activo".equals(servicio.getEstado()))
    ) throw error(HttpStatus.CONFLICT, "Selecciona un servicio activo.");

    if (request.usuarioId() == null || request.usuarioId() < 1) throw error(
      HttpStatus.BAD_REQUEST,
      "Selecciona un usuario."
    );
    var usuario = usuarioRepository
      .bloquearPorId(request.usuarioId())
      .orElseThrow(() ->
        error(HttpStatus.BAD_REQUEST, "El usuario no existe.")
      );
    boolean conservaUsuario =
      coordinador.getUsuario() != null &&
      coordinador.getUsuario().getId().equals(usuario.getId());
    if (
      "Eliminado".equals(usuario.getEstado()) ||
      (!conservaUsuario && !"Activo".equals(usuario.getEstado()))
    ) throw error(HttpStatus.CONFLICT, "Selecciona un usuario activo.");
    if (!usuarioRepository.tieneRolCoordinador(usuario.getId())) throw error(
      HttpStatus.CONFLICT,
      "El usuario debe tener el rol Coordinador."
    );
    if (
      coordinadorRepository.existsByUsuarioIdAndIdNot(
        usuario.getId(),
        coordinador.getId() == null ? 0 : coordinador.getId()
      )
    ) throw error(
      HttpStatus.CONFLICT,
      "Este usuario ya tiene una asignación de coordinador. Edita la existente, incluso si está inactiva."
    );
    coordinador.setUsuario(usuario);
    coordinador.setServicio(servicio);
  }

  public CoordinadorResponse consultar(Integer id) {
    return coordinadorMapper.toResponse(buscarCoordinador(id, false));
  }

  @Transactional
  public CoordinadorResponse crear(CoordinadorRequest request) {
    var coordinador = new Coordinador();
    validarYAsignarDatos(coordinador, request);
    return coordinadorMapper.toResponse(
      coordinadorRepository.saveAndFlush(coordinador)
    );
  }

  @Transactional
  public CoordinadorResponse actualizar(Integer id, CoordinadorRequest request) {
    var coordinador = buscarCoordinador(id, true);
    validarYAsignarDatos(coordinador, request);
    return coordinadorMapper.toResponse(
      coordinadorRepository.saveAndFlush(coordinador)
    );
  }

  @Transactional
  public CoordinadorResponse cambiarEstado(Integer id, String estado) {
    if (!"Activo".equals(estado) && !"Inactivo".equals(estado)) throw error(
      HttpStatus.BAD_REQUEST,
      "Estado inválido."
    );
    var coordinador = buscarCoordinador(id, true);
    coordinador.setEstado(estado);
    return coordinadorMapper.toResponse(
      coordinadorRepository.saveAndFlush(coordinador)
    );
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    var coordinador = buscarCoordinador(id, true);
    coordinadorRepository.delete(coordinador);
    coordinadorRepository.flush();
  }

  public List<CatalogoResponse> usuariosDisponibles() {
    return usuarioRepository
      .usuariosDisponiblesParaCoordinacion()
      .stream()
      .map(usuario ->
        new CatalogoResponse(
          usuario.getId(),
          usuario.getNombre() +
            " " +
            usuario.getApellido() +
            " — " +
            usuario.getCorreo()
        )
      )
      .toList();
  }
}
