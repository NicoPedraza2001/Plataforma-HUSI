package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.ActualizacionCredencialesRequest;
import co.edu.javeriana.husi.dto.request.ActualizacionPerfilRequest;
import co.edu.javeriana.husi.dto.request.InicioSesionRequest;
import co.edu.javeriana.husi.dto.response.PerfilResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.mapper.FotografiaAuditoriaMapper;
import co.edu.javeriana.husi.mapper.FotografiaRegistro;
import co.edu.javeriana.husi.model.entity.Usuario;
import co.edu.javeriana.husi.model.enums.AccionAuditoria;
import co.edu.javeriana.husi.model.enums.ModuloAuditoria;
import co.edu.javeriana.husi.repository.RolRepository;
import co.edu.javeriana.husi.repository.UsuarioRepository;
import co.edu.javeriana.husi.security.LimiteIntentosInicioSesion;
import co.edu.javeriana.husi.security.UsuarioEnSesion;
import co.edu.javeriana.husi.service.HistorialAuditoriaService;
import co.edu.javeriana.husi.service.SesionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SesionServiceImpl implements SesionService {

  /** Mismo mensaje para correo inexistente, contraseña errada o usuario inactivo. */
  private static final String CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos.";

  private final AuthenticationManager gestorAutenticacion;
  private final SecurityContextRepository repositorioSesion;
  private final LimiteIntentosInicioSesion limiteIntentos;
  private final UsuarioEnSesion usuarioEnSesion;
  private final RolRepository rolRepository;
  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder codificadorContrasena;
  private final HistorialAuditoriaService historialAuditoria;
  private final FotografiaAuditoriaMapper fotografias;

  @Value("${server.servlet.session.cookie.name}")
  private String nombreCookieSesion;

  @Override
  public PerfilResponse iniciarSesion(
    InicioSesionRequest credenciales,
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    String correo = credenciales.correo().strip();
    if (limiteIntentos.estaBloqueado(correo)) throw new NegocioException(
      HttpStatus.TOO_MANY_REQUESTS,
      "Demasiados intentos fallidos. Espera " +
        limiteIntentos.minutosBloqueo() +
        " minutos e intenta de nuevo."
    );

    Authentication autenticacion;
    try {
      autenticacion = gestorAutenticacion.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(correo, credenciales.contrasena())
      );
    } catch (AuthenticationException e) {
      limiteIntentos.registrarFallo(correo);
      throw new NegocioException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INCORRECTAS);
    }
    limiteIntentos.registrarExito(correo);

    guardarEnSesionNueva(autenticacion, request, response);
    return perfil();
  }

  /**
   * Descarta la sesión anterior (si existía) y crea otra con un id nuevo, para que nadie pueda
   * reutilizar un id de sesión conocido antes del inicio de sesión (fijación de sesión).
   */
  private void guardarEnSesionNueva(
    Authentication autenticacion,
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    HttpSession sesionAnterior = request.getSession(false);
    if (sesionAnterior != null) sesionAnterior.invalidate();
    request.getSession(true);

    SecurityContext contexto = SecurityContextHolder.createEmptyContext();
    contexto.setAuthentication(autenticacion);
    SecurityContextHolder.setContext(contexto);
    repositorioSesion.saveContext(contexto, request, response);
  }

  @Override
  public void cerrarSesion(HttpServletRequest request, HttpServletResponse response) {
    Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
    new SecurityContextLogoutHandler().logout(request, response, autenticacion);
    new CookieClearingLogoutHandler(nombreCookieSesion).logout(request, response, autenticacion);
  }

  @Override
  public PerfilResponse perfil() {
    Usuario usuario = usuarioEnSesion.consultar();
    return new PerfilResponse(
      usuario.getNombre(),
      usuario.getApellido(),
      usuario.getCorreo(),
      rolRepository.consultarNombre(usuario.getRolId())
    );
  }

  @Override
  @Transactional
  public PerfilResponse actualizarPerfil(ActualizacionPerfilRequest request) {
    String nombres = textoOpcional(request.nombres(), "Los nombres");
    String apellidos = textoOpcional(request.apellidos(), "Los apellidos");
    if (nombres == null && apellidos == null) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "Indica los nombres o los apellidos que quieres actualizar."
    );

    Usuario usuario = usuarioEnSesionBloqueado();
    FotografiaRegistro antes = fotografias.deUsuario(usuario);
    if (nombres != null) usuario.setNombre(nombres);
    if (apellidos != null) usuario.setApellido(apellidos);
    registrarCambio(antes, usuarioRepository.saveAndFlush(usuario), false);
    return perfil();
  }

  @Override
  @Transactional
  public PerfilResponse actualizarCredenciales(ActualizacionCredencialesRequest request) {
    String correoNuevo = request.correo() == null
      ? null
      : request.correo().strip().toLowerCase(Locale.ROOT);
    String contrasenaNueva = request.contrasenaNueva();
    if (correoNuevo == null && contrasenaNueva == null) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "Indica el correo o la contraseña que quieres actualizar."
    );

    Usuario usuario = usuarioEnSesionBloqueado();
    comprobarContrasenaActual(usuario, request.contrasenaActual());
    FotografiaRegistro antes = fotografias.deUsuario(usuario);

    if (correoNuevo != null) {
      if (correoNuevo.isEmpty()) throw new NegocioException(
        HttpStatus.BAD_REQUEST,
        "Escribe un correo electrónico válido."
      );
      if (
        usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correoNuevo, usuario.getId())
      ) throw new NegocioException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo.");
      usuario.setCorreo(correoNuevo);
    }
    if (contrasenaNueva != null) {
      if (
        contrasenaNueva.isBlank() ||
        contrasenaNueva.length() < 8 ||
        contrasenaNueva.getBytes(StandardCharsets.UTF_8).length > 72
      ) throw new NegocioException(
        HttpStatus.BAD_REQUEST,
        "La contraseña debe tener al menos 8 caracteres y no superar 72 bytes."
      );
      if (Objects.equals(contrasenaNueva, request.contrasenaActual())) throw new NegocioException(
        HttpStatus.BAD_REQUEST,
        "La nueva contraseña debe ser distinta de la actual."
      );
      usuario.setContrasena(codificadorContrasena.encode(contrasenaNueva));
    }
    registrarCambio(antes, usuarioRepository.saveAndFlush(usuario), contrasenaNueva != null);
    return perfil();
  }

  /** El usuario en sesión, bloqueado para que dos ajustes simultáneos no se pisen. */
  private Usuario usuarioEnSesionBloqueado() {
    Integer id = usuarioEnSesion.consultar().getId();
    return usuarioRepository
      .bloquearPorId(id)
      .orElseThrow(() ->
        new NegocioException(HttpStatus.UNAUTHORIZED, "No has iniciado sesión.")
      );
  }

  /**
   * Compara la contraseña escrita con el hash guardado. Un fallo suma al bloqueo por intentos del
   * correo, para que esta ruta no sirva para adivinar contraseñas.
   */
  private void comprobarContrasenaActual(Usuario usuario, String contrasenaActual) {
    String correo = usuario.getCorreo();
    if (limiteIntentos.estaBloqueado(correo)) throw new NegocioException(
      HttpStatus.TOO_MANY_REQUESTS,
      "Demasiados intentos fallidos. Espera " +
        limiteIntentos.minutosBloqueo() +
        " minutos e intenta de nuevo."
    );
    if (!codificadorContrasena.matches(contrasenaActual, usuario.getContrasena())) {
      limiteIntentos.registrarFallo(correo);
      throw new NegocioException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta.");
    }
    limiteIntentos.registrarExito(correo);
  }

  /** {@code null} si el campo no llegó (se conserva); error si llegó vacío o solo con espacios. */
  private static String textoOpcional(String valor, String campo) {
    if (valor == null) return null;
    String limpio = valor.strip();
    if (limpio.isEmpty()) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      campo + " no pueden quedar vacíos."
    );
    return limpio;
  }

  /** La contraseña nunca se guarda en el historial; solo se deja constancia del cambio. */
  private void registrarCambio(
    FotografiaRegistro antes,
    Usuario usuarioActualizado,
    boolean cambioContrasena
  ) {
    FotografiaRegistro despues = fotografias.deUsuario(usuarioActualizado);
    if (cambioContrasena) despues.datos().put("Contraseña", "Se cambió");
    historialAuditoria.registrar(
      AccionAuditoria.ACTUALIZO,
      ModuloAuditoria.USUARIOS,
      antes,
      despues
    );
  }
}
