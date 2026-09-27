package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.CoordinadorRequest;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.CoordinadorMapper;
import co.edu.javeriana.husi.model.entity.Coordinador;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.CoordinadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoordinadorServiceImpl implements CoordinadorService {

  private final CoordinadorRepository registros;
  private final ServicioRepository servicios;
  private final CoordinadorMapper mapper;
  private final UsuarioRepository usuarios;

  private NegocioException error(HttpStatus estado, String mensaje) {
    return new NegocioException(estado, mensaje);
  }

  private Coordinador obtener(Integer id, boolean bloquear) {
    return (bloquear ? registros.bloquearPorId(id) : registros.findById(id))
      .filter(e -> !"Eliminado".equals(e.getEstado()))
      .orElseThrow(() ->
        error(HttpStatus.NOT_FOUND, "No se encontró el registro.")
      );
  }

  private void aplicar(Coordinador e, CoordinadorRequest d) {
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

    if (d.usuarioId() == null || d.usuarioId() < 1) throw error(
      HttpStatus.BAD_REQUEST,
      "Selecciona un usuario."
    );
    var usuario = usuarios
      .bloquearPorId(d.usuarioId())
      .orElseThrow(() ->
        error(HttpStatus.BAD_REQUEST, "El usuario no existe.")
      );
    boolean mismoUsuario =
      e.getUsuario() != null && e.getUsuario().getId().equals(usuario.getId());
    if (
      "Eliminado".equals(usuario.getEstado()) ||
      (!mismoUsuario && !"Activo".equals(usuario.getEstado()))
    ) throw error(HttpStatus.CONFLICT, "Selecciona un usuario activo.");
    if (!usuarios.tieneRolCoordinador(usuario.getId())) throw error(
      HttpStatus.CONFLICT,
      "El usuario debe tener el rol Coordinador."
    );
    if (
      registros.existsByUsuarioIdAndIdNot(
        usuario.getId(),
        e.getId() == null ? 0 : e.getId()
      )
    ) throw error(
      HttpStatus.CONFLICT,
      "Este usuario ya tiene una asignación de coordinador. Edita la existente, incluso si está inactiva."
    );
    e.setUsuario(usuario);

    e.setServicio(servicio);
  }

  public CoordinadorResponse consultar(Integer id) {
    return mapper.response(obtener(id, false));
  }

  @Transactional
  public CoordinadorResponse crear(CoordinadorRequest d) {
    var e = new Coordinador();
    aplicar(e, d);
    return mapper.response(registros.saveAndFlush(e));
  }

  @Transactional
  public CoordinadorResponse actualizar(Integer id, CoordinadorRequest d) {
    var e = obtener(id, true);
    aplicar(e, d);
    return mapper.response(registros.saveAndFlush(e));
  }

  @Transactional
  public CoordinadorResponse cambiarEstado(Integer id, String estado) {
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
    registros.delete(e);
    registros.flush();
  }

  public java.util.List<CatalogoResponse> usuariosDisponibles() {
    return usuarios
      .usuariosDisponiblesParaCoordinacion()
      .stream()
      .map(u ->
        new CatalogoResponse(
          u.getId(),
          u.getNombre() + " " + u.getApellido() + " — " + u.getCorreo()
        )
      )
      .toList();
  }
}
