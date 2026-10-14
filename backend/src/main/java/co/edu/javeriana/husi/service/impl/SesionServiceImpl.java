package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.InicioSesionRequest;
import co.edu.javeriana.husi.dto.response.PerfilResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.model.entity.Usuario;
import co.edu.javeriana.husi.repository.RolRepository;
import co.edu.javeriana.husi.security.LimiteIntentosInicioSesion;
import co.edu.javeriana.husi.security.UsuarioEnSesion;
import co.edu.javeriana.husi.service.SesionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
}
