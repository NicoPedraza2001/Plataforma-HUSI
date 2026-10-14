package co.edu.javeriana.husi.controller;

import static co.edu.javeriana.husi.ClienteApiDePrueba.CONTRASENA_ADMINISTRADOR;
import static co.edu.javeriana.husi.ClienteApiDePrueba.CORREO_ADMINISTRADOR;
import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.ClienteApiDePrueba;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Inicio de sesión, roles, CSRF y bloqueo por intentos fallidos contra la API real. */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "spring.datasource.url=jdbc:h2:mem:sesion;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "husi.seguridad.administrador-inicial.contrasena=" + CONTRASENA_ADMINISTRADOR,
  }
)
@ActiveProfiles("local")
class SesionH2IntegrationTests {

  private static final String CONTRASENA_COORDINADOR = "Coordinador-Prueba-1";
  private static final int ID_ROL_COORDINADOR = 2;

  @LocalServerPort
  int puerto;

  @Autowired
  JdbcTemplate jdbc;

  /** Crea un coordinador con la sesión del administrador y devuelve su correo. */
  private String crearCoordinador() throws Exception {
    String correo = "coordinador-" + UUID.randomUUID() + "@example.test";
    var respuesta = ClienteApiDePrueba.conSesionDeAdministrador(puerto).enviar(
      "POST",
      "usuarios",
      "{\"nombre\":\"Coordinador\",\"apellido\":\"Prueba\",\"correo\":\"" +
        correo +
        "\",\"rolId\":" +
        ID_ROL_COORDINADOR +
        ",\"contrasena\":\"" +
        CONTRASENA_COORDINADOR +
        "\"}"
    );
    assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(201);
    return correo;
  }

  @Test
  void sinSesionLaApiResponde401() throws Exception {
    var anonimo = new ClienteApiDePrueba(puerto);
    for (String ruta : new String[] { "universidades", "usuarios", "historial", "sesion/perfil" }) {
      var respuesta = anonimo.get(ruta);
      assertThat(respuesta.statusCode()).as(ruta).isEqualTo(401);
      assertThat(respuesta.body()).contains("no has iniciado sesión");
    }
  }

  @Test
  void elAdministradorIniciaSesionYEntraAlPanel() throws Exception {
    var cliente = new ClienteApiDePrueba(puerto);
    var respuesta = cliente.iniciarSesion(CORREO_ADMINISTRADOR, CONTRASENA_ADMINISTRADOR);

    assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(200);
    assertThat(respuesta.body())
      .contains("\"rol\":\"Administrador\"")
      .doesNotContain("contrasena")
      .doesNotContain("$2");
    String cookieSesion = respuesta
      .headers()
      .allValues("Set-Cookie")
      .stream()
      .filter(cookie -> cookie.startsWith("HUSI_SESION="))
      .findFirst()
      .orElseThrow();
    assertThat(cookieSesion).contains("HttpOnly").contains("SameSite=Strict");
    assertThat(cliente.get("universidades").statusCode()).isEqualTo(200);
    assertThat(cliente.get("sesion/perfil").body()).contains(CORREO_ADMINISTRADOR);
  }

  @Test
  void elTokenCsrfLlegaEnUnaCookieQueAngularPuedeLeerDesdeCualquierPagina() throws Exception {
    String cookieCsrf = new ClienteApiDePrueba(puerto)
      .get("sesion/perfil")
      .headers()
      .allValues("Set-Cookie")
      .stream()
      .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
      .findFirst()
      .orElseThrow();
    // Con Path=/api, Angular no la vería desde /login y el inicio de sesión fallaría con 403.
    assertThat(cookieCsrf).contains("Path=/;").doesNotContain("HttpOnly");
  }

  @Test
  void unaPeticionSinSesionNoAbreSesionEnElServidor() throws Exception {
    var respuesta = new ClienteApiDePrueba(puerto).get("universidades");
    assertThat(respuesta.headers().allValues("Set-Cookie")).noneMatch(cookie ->
      cookie.startsWith("HUSI_SESION=")
    );
  }

  @Test
  void laContrasenaSeGuardaComoHashBcrypt() {
    String guardada = jdbc.queryForObject(
      "select contrasena from usuario where correo=?",
      String.class,
      CORREO_ADMINISTRADOR
    );
    assertThat(guardada).startsWith("$2").doesNotContain(CONTRASENA_ADMINISTRADOR);
  }

  @Test
  void credencialesIncorrectasDanElMismoMensajeExistaONoElCorreo() throws Exception {
    var cliente = new ClienteApiDePrueba(puerto);
    var contrasenaErrada = cliente.iniciarSesion(CORREO_ADMINISTRADOR + ".x", "otra-cosa-123");
    var correoInexistente = cliente.iniciarSesion("nadie@example.test", "otra-cosa-123");

    assertThat(contrasenaErrada.statusCode()).isEqualTo(401);
    assertThat(correoInexistente.statusCode()).isEqualTo(401);
    assertThat(contrasenaErrada.body()).isEqualTo(correoInexistente.body());
    assertThat(cliente.get("universidades").statusCode()).isEqualTo(401);
  }

  @Test
  void elCoordinadorNoEntraAlPanelDeAdministrador() throws Exception {
    String correo = crearCoordinador();
    var coordinador = new ClienteApiDePrueba(puerto);
    var respuesta = coordinador.iniciarSesion(correo, CONTRASENA_COORDINADOR);

    assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(200);
    assertThat(respuesta.body()).contains("\"rol\":\"Coordinador\"");
    assertThat(coordinador.get("sesion/perfil").statusCode()).isEqualTo(200);
    assertThat(coordinador.get("universidades").statusCode()).isEqualTo(403);
    assertThat(coordinador.get("usuarios").statusCode()).isEqualTo(403);
  }

  @Test
  void bloqueaElCorreoTrasVariosIntentosFallidos() throws Exception {
    String correo = crearCoordinador();
    var atacante = new ClienteApiDePrueba(puerto);
    for (int intento = 1; intento <= 5; intento++) assertThat(
      atacante.iniciarSesion(correo, "adivinanza-" + intento).statusCode()
    ).isEqualTo(401);

    // Bloqueado: ni siquiera la contraseña correcta entra.
    var bloqueado = new ClienteApiDePrueba(puerto).iniciarSesion(correo, CONTRASENA_COORDINADOR);
    assertThat(bloqueado.statusCode()).isEqualTo(429);
    assertThat(bloqueado.body()).contains("Demasiados intentos");
  }

  @Test
  void sinTokenCsrfNoSeModificanDatos() throws Exception {
    var cliente = ClienteApiDePrueba.conSesionDeAdministrador(puerto);
    var respuesta = cliente.enviarSinTokenCsrf(
      "POST",
      "universidades",
      "{\"nombre\":\"Universidad CSRF\",\"pais\":\"Colombia\"}"
    );
    assertThat(respuesta.statusCode()).isEqualTo(403);
    assertThat(
      jdbc.queryForObject(
        "select count(*) from universidad where nombre_universidad='Universidad CSRF'",
        Integer.class
      )
    ).isZero();
  }

  @Test
  void cerrarSesionInvalidaLaSesion() throws Exception {
    var cliente = ClienteApiDePrueba.conSesionDeAdministrador(puerto);
    assertThat(cliente.enviar("POST", "sesion/cerrar", "").statusCode()).isEqualTo(204);
    assertThat(cliente.get("sesion/perfil").statusCode()).isEqualTo(401);
    assertThat(cliente.get("universidades").statusCode()).isEqualTo(401);
  }
}
