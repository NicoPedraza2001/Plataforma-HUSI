package co.edu.javeriana.husi.security;

import co.edu.javeriana.husi.model.enums.RolUsuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

/**
 * Reglas de acceso de toda la API.
 *
 * <ul>
 *   <li><b>Sesión en el servidor</b>: al iniciar sesión el backend guarda el usuario en la sesión
 *       HTTP y el navegador solo recibe la cookie {@code HUSI_SESION} (HttpOnly, SameSite=Strict),
 *       que JavaScript no puede leer. No se entrega ningún token al frontend.
 *   <li><b>CSRF</b>: las peticiones que modifican datos deben llevar la cabecera
 *       {@code X-XSRF-TOKEN} con el valor de la cookie {@code XSRF-TOKEN}; Angular lo hace solo.
 *   <li><b>Roles</b>: {@code /sesion/**} es para cualquier usuario autenticado; el resto de la
 *       API, por ahora, solo para {@link RolUsuario#ADMINISTRADOR}. Al construir el panel del
 *       coordinador, abrir aquí las rutas que necesite.
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

  /** Costo de BCrypt: 2^12 iteraciones (~250 ms por comparación), lento a propósito. */
  private static final int COSTO_BCRYPT = 12;

  @Bean
  SecurityFilterChain cadenaDeSeguridad(
    HttpSecurity http,
    RespuestaErrorSeguridad respuestaError,
    SecurityContextRepository repositorioSesion,
    @Value("${husi.seguridad.documentacion-publica}") boolean documentacionPublica
  ) throws Exception {
    http
      .cors(Customizer.withDefaults())
      .csrf(csrf -> csrf.spa().csrfTokenRepository(repositorioTokenCsrf()))
      .addFilterAfter(new CsrfCookieFiltro(), CsrfFilter.class)
      .securityContext(contexto -> contexto.securityContextRepository(repositorioSesion))
      // Una petición rechazada no abre sesión: solo POST /sesion/iniciar crea una.
      .requestCache(cache -> cache.disable())
      // El inicio y el cierre de sesión los atiende SesionController con respuestas JSON.
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable)
      .logout(AbstractHttpConfigurer::disable)
      .exceptionHandling(errores ->
        errores
          .authenticationEntryPoint(respuestaError::responderSinSesion)
          .accessDeniedHandler(respuestaError::responderSinPermiso)
      )
      .authorizeHttpRequests(rutas -> {
        rutas.requestMatchers("/error").permitAll();
        rutas.requestMatchers(HttpMethod.POST, "/sesion/iniciar").permitAll();
        if (documentacionPublica) rutas
          .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
          .permitAll();
        rutas.requestMatchers("/sesion/**").authenticated();
        rutas.anyRequest().hasRole(RolUsuario.ADMINISTRADOR.name());
      });
    return http.build();
  }

  /**
   * Token CSRF en la cookie {@code XSRF-TOKEN}, legible por JavaScript (es su propósito: Angular
   * la copia en la cabecera). Va en la ruta {@code /} y no en {@code /api}, porque Angular la lee
   * desde páginas como {@code /login}.
   */
  private CookieCsrfTokenRepository repositorioTokenCsrf() {
    var repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repositorio.setCookiePath("/");
    repositorio.setCookieCustomizer(cookie -> cookie.sameSite("Strict"));
    return repositorio;
  }

  /** Guarda el usuario autenticado en la sesión HTTP entre una petición y otra. */
  @Bean
  SecurityContextRepository repositorioSesion() {
    return new HttpSessionSecurityContextRepository();
  }

  /** Hash de contraseñas. Todas las contraseñas se guardan con BCrypt, nunca en texto plano. */
  @Bean
  PasswordEncoder codificadorContrasena() {
    return new BCryptPasswordEncoder(COSTO_BCRYPT);
  }

  @Bean
  AuthenticationManager gestorAutenticacion(
    CargadorUsuarioAutenticado cargadorUsuario,
    PasswordEncoder codificadorContrasena
  ) {
    var proveedor = new DaoAuthenticationProvider(cargadorUsuario);
    proveedor.setPasswordEncoder(codificadorContrasena);
    return new ProviderManager(proveedor);
  }
}
