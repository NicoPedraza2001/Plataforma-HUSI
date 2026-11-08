package co.edu.javeriana.husi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.javeriana.husi.model.enums.RolUsuario;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** Prueba unitaria de la revisión de la sesión en cada petición (RF-03, RNF-08). */
class SesionVigenteFiltroTest {

  private static final String CORREO = "persona@example.test";
  private static final String HASH = "hash-de-la-contrasena";

  private final CargadorUsuarioAutenticado cargadorUsuario = mock(CargadorUsuarioAutenticado.class);
  private final SesionVigenteFiltro filtro = new SesionVigenteFiltro(cargadorUsuario);
  private final MockHttpSession sesion = new MockHttpSession();
  private final MockHttpServletRequest peticion = new MockHttpServletRequest();
  private final FilterChain cadena = mock(FilterChain.class);

  @AfterEach
  void limpiarSesionDelHilo() {
    SecurityContextHolder.clearContext();
  }

  private UsuarioAutenticado cuenta(String correo, String hash, RolUsuario rol, boolean activa) {
    return new UsuarioAutenticado(7, correo, hash, rol, activa);
  }

  private void abrirSesion() {
    var abierta = cuenta(CORREO, HASH, RolUsuario.ADMINISTRADOR, true);
    SecurityContextHolder.getContext().setAuthentication(
      UsernamePasswordAuthenticationToken.authenticated(abierta, null, abierta.getAuthorities())
    );
    peticion.setSession(sesion);
  }

  private void filtrar() throws Exception {
    var respuesta = new MockHttpServletResponse();
    filtro.doFilter(peticion, respuesta, cadena);
    verify(cadena).doFilter(peticion, respuesta);
  }

  private void debeTerminarLaSesion() {
    assertThat(sesion.isInvalid()).isTrue();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("CU01 Sin sesión no se consulta la base y la petición sigue")
  void sinSesionNoConsultaLaBase() throws Exception {
    filtrar();
    verifyNoInteractions(cargadorUsuario);
  }

  @Test
  @DisplayName("CU01 Si la cuenta no cambió, la sesión sigue abierta")
  void siLaCuentaNoCambioLaSesionSigue() throws Exception {
    abrirSesion();
    when(cargadorUsuario.loadUserByUsername(CORREO)).thenReturn(
      cuenta(CORREO, HASH, RolUsuario.ADMINISTRADOR, true)
    );

    filtrar();

    assertThat(sesion.isInvalid()).isFalse();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
  }

  @Test
  @DisplayName("CU01-EX10 Si la cuenta se desactivó, la sesión termina")
  void siLaCuentaSeDesactivoLaSesionTermina() throws Exception {
    abrirSesion();
    when(cargadorUsuario.loadUserByUsername(CORREO)).thenReturn(
      cuenta(CORREO, HASH, RolUsuario.ADMINISTRADOR, false)
    );
    filtrar();
    debeTerminarLaSesion();
  }

  @Test
  @DisplayName("CU01-EX10 Si la cuenta se eliminó, la sesión termina")
  void siLaCuentaSeEliminoLaSesionTermina() throws Exception {
    abrirSesion();
    when(cargadorUsuario.loadUserByUsername(CORREO)).thenThrow(
      new UsernameNotFoundException("Usuario no encontrado.")
    );
    filtrar();
    debeTerminarLaSesion();
  }

  @Test
  @DisplayName("CU01-EX11 Si la cuenta cambió de rol, la sesión termina")
  void siLaCuentaCambioDeRolLaSesionTermina() throws Exception {
    abrirSesion();
    when(cargadorUsuario.loadUserByUsername(CORREO)).thenReturn(
      cuenta(CORREO, HASH, RolUsuario.COORDINADOR, true)
    );
    filtrar();
    debeTerminarLaSesion();
  }

  @Test
  @DisplayName("CU02-EX8 Si cambió la contraseña, la sesión termina")
  void siCambioLaContrasenaLaSesionTermina() throws Exception {
    abrirSesion();
    when(cargadorUsuario.loadUserByUsername(CORREO)).thenReturn(
      cuenta(CORREO, "otro-hash", RolUsuario.ADMINISTRADOR, true)
    );
    filtrar();
    debeTerminarLaSesion();
  }
}
