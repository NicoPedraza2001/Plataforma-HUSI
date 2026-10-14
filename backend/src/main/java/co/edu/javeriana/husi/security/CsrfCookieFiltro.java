package co.edu.javeriana.husi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security genera el token CSRF solo cuando alguien lo lee. Este filtro lo lee en cada
 * petición para que la cookie {@code XSRF-TOKEN} llegue al navegador desde la primera consulta
 * (p. ej. {@code GET /sesion/perfil}) y Angular pueda enviarlo al iniciar sesión.
 */
class CsrfCookieFiltro extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain cadenaDeFiltros
  ) throws ServletException, IOException {
    if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) token.getToken();
    cadenaDeFiltros.doFilter(request, response);
  }
}
