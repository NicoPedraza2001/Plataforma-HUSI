package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.UsuarioRequest;
import co.edu.javeriana.husi.dto.response.UsuarioResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.UsuarioMapper;
import co.edu.javeriana.husi.model.entity.Usuario;
import co.edu.javeriana.husi.repository.*;
import co.edu.javeriana.husi.service.UsuarioService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final RolRepository rolRepository;
  private final UsuarioMapper usuarioMapper;
  private final BCryptPasswordEncoder codificadorContrasena =
    new BCryptPasswordEncoder();

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
    if (
      usuario.getId() != null &&
      !Objects.equals(usuario.getRolId(), request.rolId()) &&
      usuarioRepository.tieneCoordinacion(usuario.getId()) &&
      !rolRepository.esCoordinador(request.rolId())
    ) throw new NegocioException(
      HttpStatus.CONFLICT,
      "El usuario tiene una asignación de coordinador. Retírala antes de cambiar su rol."
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

  @Transactional
  public UsuarioResponse consultar(Integer id) {
    return usuarioMapper.toResponse(buscarUsuarioBloqueado(id));
  }

  @Transactional
  public UsuarioResponse crear(UsuarioRequest request) {
    var usuario = new Usuario();
    validarYAsignarDatos(usuario, request);
    return usuarioMapper.toResponse(usuarioRepository.saveAndFlush(usuario));
  }

  @Transactional
  public UsuarioResponse actualizar(Integer id, UsuarioRequest request) {
    var usuario = buscarUsuarioBloqueado(id);
    validarYAsignarDatos(usuario, request);
    return usuarioMapper.toResponse(usuarioRepository.saveAndFlush(usuario));
  }

  @Transactional
  public UsuarioResponse cambiarEstado(Integer id, String estado) {
    if (
      !"Activo".equals(estado) && !"Inactivo".equals(estado)
    ) throw new NegocioException(HttpStatus.BAD_REQUEST, "Estado inválido.");
    var usuario = buscarUsuarioBloqueado(id);
    usuario.setEstado(estado);
    return usuarioMapper.toResponse(usuarioRepository.saveAndFlush(usuario));
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    var usuario = buscarUsuarioBloqueado(id);
    if (usuarioRepository.contarReferencias(id) > 0) throw new NegocioException(
      HttpStatus.CONFLICT,
      "El usuario tiene registros de coordinación o auditoría asociados. Puedes desactivarlo para conservar su historial."
    );
    usuarioRepository.delete(usuario);
    usuarioRepository.flush();
  }
}
