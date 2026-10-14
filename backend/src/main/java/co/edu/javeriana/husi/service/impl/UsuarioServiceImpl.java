package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.UsuarioRequest;
import co.edu.javeriana.husi.dto.response.UsuarioResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.FotografiaAuditoriaMapper;
import co.edu.javeriana.husi.mapper.FotografiaRegistro;
import co.edu.javeriana.husi.mapper.UsuarioMapper;
import co.edu.javeriana.husi.model.entity.Coordinador;
import co.edu.javeriana.husi.model.entity.Servicio;
import co.edu.javeriana.husi.model.entity.Usuario;
import co.edu.javeriana.husi.model.enums.AccionAuditoria;
import co.edu.javeriana.husi.model.enums.ModuloAuditoria;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.HistorialAuditoriaService;
import co.edu.javeriana.husi.service.UsuarioService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final RolRepository rolRepository;
  private final CoordinadorRepository coordinadorRepository;
  private final ServicioRepository servicioRepository;
  private final UsuarioMapper usuarioMapper;
  private final HistorialAuditoriaService historialAuditoria;
  private final FotografiaAuditoriaMapper fotografias;
  private final PasswordEncoder codificadorContrasena;

  private Usuario buscarUsuarioBloqueado(Integer id) {
    return usuarioRepository
      .bloquearPorId(id)
      .filter(usuario -> !"Eliminado".equals(usuario.getEstado()))
      .orElseThrow(() ->
        new NegocioException(HttpStatus.NOT_FOUND, "No se encontró el usuario.")
      );
  }

  /** Valida la solicitud y copia sus datos sobre el usuario (nuevo o existente). */
  private void validarYAsignarDatos(Usuario usuario, UsuarioRequest request) {
    if (!rolRepository.existe(request.rolId())) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "El rol no existe."
    );
    String correo = request.correo().strip().toLowerCase(Locale.ROOT);
    if (
      usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(
        correo,
        usuario.getId() == null ? 0 : usuario.getId()
      )
    ) throw new NegocioException(
      HttpStatus.CONFLICT,
      "Ya existe un usuario con ese correo."
    );
    // Al editar, una contraseña vacía conserva la actual.
    String contrasena = request.contrasena();
    boolean esNuevo = usuario.getId() == null;
    if (esNuevo || (contrasena != null && !contrasena.isEmpty())) {
      if (
        contrasena == null ||
        contrasena.isBlank() ||
        contrasena.length() < 8 ||
        contrasena.getBytes(StandardCharsets.UTF_8).length > 72
      ) throw new NegocioException(
        HttpStatus.BAD_REQUEST,
        "La contraseña debe tener al menos 8 caracteres y no superar 72 bytes."
      );
      usuario.setContrasena(codificadorContrasena.encode(contrasena));
    }
    usuario.setNombre(request.nombre().strip());
    usuario.setApellido(request.apellido().strip());
    usuario.setCorreo(correo);
    usuario.setRolId(request.rolId());
  }

  /**
   * Servicio que coordinará el usuario, o {@code null} si su rol no es Coordinador. Un servicio
   * inactivo solo se acepta si el usuario ya lo coordinaba. Por ahora un servicio puede tener
   * varios coordinadores.
   */
  private Servicio validarServicioCoordinado(Usuario usuario, UsuarioRequest request) {
    if (!rolRepository.esCoordinador(request.rolId())) return null;
    if (request.servicioId() == null) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "Selecciona el servicio que coordinará el usuario."
    );
    var servicio = servicioRepository
      .bloquearPorId(request.servicioId())
      .orElseThrow(() -> new NegocioException(HttpStatus.BAD_REQUEST, "El servicio no existe."));
    boolean conservaServicio =
      usuario.getId() != null &&
      coordinadorRepository
        .findByUsuarioId(usuario.getId())
        .map(coordinador -> coordinador.getServicio().getId().equals(servicio.getId()))
        .orElse(false);
    if (
      "Eliminado".equals(servicio.getEstado()) ||
      (!conservaServicio && !"Activo".equals(servicio.getEstado()))
    ) throw new NegocioException(HttpStatus.CONFLICT, "Selecciona un servicio activo.");
    return servicio;
  }

  /**
   * Deja la tabla {@code coordinador} de acuerdo con el usuario: crea o cambia su servicio si es
   * coordinador, la retira si dejó de serlo, y copia su estado (el dashboard cuenta los
   * coordinadores activos de cada servicio).
   */
  private void sincronizarCoordinacion(Usuario usuario, Servicio servicioCoordinado) {
    var coordinacion = coordinadorRepository.findByUsuarioId(usuario.getId());
    if (servicioCoordinado == null) {
      coordinacion.ifPresent(coordinadorRepository::delete);
      coordinadorRepository.flush();
      return;
    }
    var coordinador = coordinacion.orElseGet(Coordinador::new);
    coordinador.setUsuario(usuario);
    coordinador.setServicio(servicioCoordinado);
    coordinador.setEstado(usuario.getEstado());
    coordinadorRepository.saveAndFlush(coordinador);
  }

  @Transactional
  public UsuarioResponse consultar(Integer id) {
    return usuarioMapper.toResponse(buscarUsuarioBloqueado(id));
  }

  @Transactional
  public UsuarioResponse crear(UsuarioRequest request) {
    var usuario = new Usuario();
    Servicio servicioCoordinado = validarServicioCoordinado(usuario, request);
    validarYAsignarDatos(usuario, request);
    var usuarioCreado = usuarioRepository.saveAndFlush(usuario);
    sincronizarCoordinacion(usuarioCreado, servicioCoordinado);
    historialAuditoria.registrar(
      AccionAuditoria.CREO,
      ModuloAuditoria.USUARIOS,
      null,
      fotografias.deUsuario(usuarioCreado)
    );
    return usuarioMapper.toResponse(usuarioCreado);
  }

  @Transactional
  public UsuarioResponse actualizar(Integer id, UsuarioRequest request) {
    var usuario = buscarUsuarioBloqueado(id);
    FotografiaRegistro antes = fotografias.deUsuario(usuario);
    Servicio servicioCoordinado = validarServicioCoordinado(usuario, request);
    validarYAsignarDatos(usuario, request);
    var usuarioActualizado = usuarioRepository.saveAndFlush(usuario);
    sincronizarCoordinacion(usuarioActualizado, servicioCoordinado);
    FotografiaRegistro despues = fotografias.deUsuario(usuarioActualizado);
    // La contraseña nunca se guarda en el historial; solo se deja constancia del cambio.
    boolean cambioContrasena =
      request.contrasena() != null && !request.contrasena().isEmpty();
    if (cambioContrasena) despues.datos().put("Contraseña", "Se cambió");
    historialAuditoria.registrar(
      AccionAuditoria.ACTUALIZO,
      ModuloAuditoria.USUARIOS,
      antes,
      despues
    );
    return usuarioMapper.toResponse(usuarioActualizado);
  }

  @Transactional
  public UsuarioResponse cambiarEstado(Integer id, String estado) {
    if (
      !"Activo".equals(estado) && !"Inactivo".equals(estado)
    ) throw new NegocioException(HttpStatus.BAD_REQUEST, "Estado inválido.");
    var usuario = buscarUsuarioBloqueado(id);
    FotografiaRegistro antes = fotografias.deUsuario(usuario);
    usuario.setEstado(estado);
    var usuarioActualizado = usuarioRepository.saveAndFlush(usuario);
    coordinadorRepository
      .findByUsuarioId(id)
      .ifPresent(coordinador -> {
        coordinador.setEstado(estado);
        coordinadorRepository.saveAndFlush(coordinador);
      });
    historialAuditoria.registrar(
      AccionAuditoria.segunEstado(estado),
      ModuloAuditoria.USUARIOS,
      antes,
      fotografias.deUsuario(usuarioActualizado)
    );
    return usuarioMapper.toResponse(usuarioActualizado);
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    var usuario = buscarUsuarioBloqueado(id);
    if (usuarioRepository.contarReferencias(id) > 0) throw new NegocioException(
      HttpStatus.CONFLICT,
      "El usuario tiene movimientos en el historial de auditoría. Puedes desactivarlo para conservar su historial."
    );
    FotografiaRegistro antes = fotografias.deUsuario(usuario);
    // Su asignación de servicio no es historia propia: se va con el usuario.
    sincronizarCoordinacion(usuario, null);
    usuarioRepository.delete(usuario);
    usuarioRepository.flush();
    historialAuditoria.registrar(
      AccionAuditoria.ELIMINO,
      ModuloAuditoria.USUARIOS,
      antes,
      null
    );
  }
}
