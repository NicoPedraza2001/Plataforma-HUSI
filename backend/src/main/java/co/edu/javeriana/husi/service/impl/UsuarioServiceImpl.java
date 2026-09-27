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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final UsuarioMapper mapper;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  private Usuario obtener(Integer id) {
    return usuarios
      .bloquearPorId(id)
      .filter(e -> !"Eliminado".equals(e.getEstado()))
      .orElseThrow(() ->
        new NegocioException(HttpStatus.NOT_FOUND, "No se encontró el usuario.")
      );
  }

  private void aplicar(Usuario e, UsuarioRequest d) {
    if (!roles.existe(d.rolId())) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "El rol no existe."
    );
    if (
      e.getId() != null &&
      !java.util.Objects.equals(e.getRolId(), d.rolId()) &&
      usuarios.tieneCoordinacion(e.getId()) &&
      !roles.esCoordinador(d.rolId())
    ) throw new NegocioException(
      HttpStatus.CONFLICT,
      "El usuario tiene una asignación de coordinador. Retírala antes de cambiar su rol."
    );
    String correo = d.correo().strip().toLowerCase(Locale.ROOT);
    if (
      usuarios.existsByCorreoIgnoreCaseAndIdNot(
        correo,
        e.getId() == null ? 0 : e.getId()
      )
    ) throw new NegocioException(
      HttpStatus.CONFLICT,
      "Ya existe un usuario con ese correo."
    );
    String clave = d.contrasena();
    if (e.getId() == null || (clave != null && !clave.isEmpty())) {
      if (
        clave == null ||
        clave.isBlank() ||
        clave.length() < 8 ||
        clave.getBytes(StandardCharsets.UTF_8).length > 72
      ) throw new NegocioException(
        HttpStatus.BAD_REQUEST,
        "La contraseña debe tener al menos 8 caracteres y no superar 72 bytes."
      );
      e.setContrasena(encoder.encode(clave));
    }
    e.setNombre(d.nombre().strip());
    e.setApellido(d.apellido().strip());
    e.setCorreo(correo);
    e.setRolId(d.rolId());
  }

  @Transactional
  public UsuarioResponse consultar(Integer id) {
    return mapper.response(obtener(id));
  }

  @Transactional
  public UsuarioResponse crear(UsuarioRequest d) {
    var e = new Usuario();
    aplicar(e, d);
    return mapper.response(usuarios.saveAndFlush(e));
  }

  @Transactional
  public UsuarioResponse actualizar(Integer id, UsuarioRequest d) {
    var e = obtener(id);
    aplicar(e, d);
    return mapper.response(usuarios.saveAndFlush(e));
  }

  @Transactional
  public UsuarioResponse cambiarEstado(Integer id, String estado) {
    if (
      !"Activo".equals(estado) && !"Inactivo".equals(estado)
    ) throw new NegocioException(HttpStatus.BAD_REQUEST, "Estado inválido.");
    var e = obtener(id);
    e.setEstado(estado);
    return mapper.response(usuarios.saveAndFlush(e));
  }

  @Transactional
  public void eliminarDefinitivamente(Integer id) {
    var e = obtener(id);
    if (usuarios.contarReferencias(id) > 0) throw new NegocioException(
      HttpStatus.CONFLICT,
      "El usuario tiene registros de coordinación o auditoría asociados. Puedes desactivarlo para conservar su historial."
    );
    usuarios.delete(e);
    usuarios.flush();
  }
}
