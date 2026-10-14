package co.edu.javeriana.husi.security;

import co.edu.javeriana.husi.exception.ApiExceptionHandler.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Responde con el mismo JSON de {@code ApiExceptionHandler} cuando Spring Security corta una
 * petición antes de llegar al controller, para que el frontend muestre el mensaje con
 * {@code mensajeError()}.
 */
@Component
@RequiredArgsConstructor
public class RespuestaErrorSeguridad {

  private final ObjectMapper jsonMapper;

  /** 401: no hay sesión o expiró. */
  public void responderSinSesion(
    HttpServletRequest request,
    HttpServletResponse response,
    AuthenticationException excepcion
  ) throws IOException {
    escribir(response, HttpStatus.UNAUTHORIZED, "Tu sesión expiró o no has iniciado sesión.");
  }

  /** 403: hay sesión, pero el rol no tiene permiso, o falta el token CSRF. */
  public void responderSinPermiso(
    HttpServletRequest request,
    HttpServletResponse response,
    AccessDeniedException excepcion
  ) throws IOException {
    escribir(
      response,
      HttpStatus.FORBIDDEN,
      excepcion instanceof CsrfException
        ? "La página quedó desactualizada. Recárgala e intenta de nuevo."
        : "Tu perfil no tiene permiso para realizar esta acción."
    );
  }

  private void escribir(HttpServletResponse response, HttpStatus estado, String mensaje)
    throws IOException {
    response.setStatus(estado.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    jsonMapper.writeValue(response.getWriter(), new ErrorResponse(mensaje, Map.of()));
  }
}
