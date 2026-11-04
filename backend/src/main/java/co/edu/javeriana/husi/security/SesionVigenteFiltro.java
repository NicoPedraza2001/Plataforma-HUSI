package co.edu.javeriana.husi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * La sesión guarda el rol que tenía el usuario al ingresar. Este filtro compara en cada petición
 * esa sesión con la cuenta en la base de datos: si la cuenta se eliminó, se desactivó, cambió de
 * rol o cambió de correo o contraseña, la sesión termina y la petición sigue como anónima, de modo
 * que las reglas de {@link ConfiguracionSeguridad} responden 401.
 */
class SesionVigenteFiltro extends OncePerRequestFilter {

  private final CargadorUsuarioAutenticado cargadorUsuario;

  SesionVigenteFiltro(CargadorUsuarioAutenticado cargadorUsuario) {
    this.cargadorUsuario = cargadorUsuario;
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain cadenaDeFiltros
  ) throws ServletException, IOException {
    Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
    if (
      autenticacion != null &&
      autenticacion.getPrincipal() instanceof UsuarioAutenticado sesionAbierta &&
      !sigueVigente(sesionAbierta)
    ) terminarSesion(request);
    cadenaDeFiltros.doFilter(request, response);
  }

  private boolean sigueVigente(UsuarioAutenticado sesionAbierta) {
    try {
      return cargadorUsuario
        .loadUserByUsername(sesionAbierta.getUsername())
        .conservaElAccesoDe(sesionAbierta);
    } catch (UsernameNotFoundException cuentaSinAcceso) {
      return false;
    }
  }

  private void terminarSesion(HttpServletRequest request) {
    HttpSession sesion = request.getSession(false);
    if (sesion != null) sesion.invalidate();
    SecurityContextHolder.clearContext();
  }
}
