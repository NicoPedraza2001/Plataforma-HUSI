package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/** CU-01 Iniciar y cerrar sesión (RF-01, RF-02, RF-03, RNF-07, RNF-10). */
class CU01SesionH2IntegrationTests extends BaseCasoDeUsoPorApi {

  private static final String CONTRASENA_ERRADA = "No-Es-La-Clave-9";

  @Value("${server.servlet.session.timeout}")
  Duration duracionDeSesion;

  private void fallarIntentos(String correo, int intentos) throws Exception {
    var cliente = clienteSinSesion();
    for (int i = 0; i < intentos; i++) assertThat(
      cliente.iniciarSesion(correo, CONTRASENA_ERRADA).statusCode()
    ).isEqualTo(401);
  }

  private String cookieDeSesion(HttpResponse<String> respuesta) {
    return respuesta
      .headers()
      .allValues("set-cookie")
      .stream()
      .filter(cookie -> cookie.startsWith("HUSI_SESION="))
      .findFirst()
      .orElseThrow();
  }

  @Test
  @DisplayName("CU01-FP2 El coordinador ingresa y recibe su perfil con su rol")
  void elCoordinadorIngresaYRecibeSuRol() throws Exception {
    var cuenta = crearCoordinador();
    var respuesta = clienteSinSesion().iniciarSesion(cuenta.correo(), CONTRASENA_DE_CUENTA);
    assertThat(respuesta.statusCode()).isEqualTo(200);
    assertThat(respuesta.body()).contains("\"rol\":\"Coordinador\"", cuenta.correo());
    assertThat(clienteDe(cuenta).get("coordinador/residentes").statusCode()).isEqualTo(200);
  }

  @Test
  @DisplayName("CU01-FA1 Un ingreso correcto reinicia el contador de intentos fallidos")
  void unIngresoCorrectoReiniciaElContador() throws Exception {
    var cuenta = crearCoordinador();
    fallarIntentos(cuenta.correo(), 4);
    clienteDe(cuenta);
    fallarIntentos(cuenta.correo(), 4);
    assertThat(clienteDe(cuenta).get("sesion/perfil").statusCode()).isEqualTo(200);
  }

  @Test
  @DisplayName("CU01-FA2 El correo se acepta sin distinguir mayúsculas")
  void elCorreoNoDistingueMayusculas() throws Exception {
    var cuenta = crearCoordinador();
    var respuesta = clienteSinSesion().iniciarSesion(
      cuenta.correo().toUpperCase(),
      CONTRASENA_DE_CUENTA
    );
    assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(200);
  }

  @Test
  @DisplayName("CU01-EX2 Un usuario inactivo no ingresa y recibe el mismo mensaje")
  void unUsuarioInactivoNoIngresa() throws Exception {
    var cuenta = crearCoordinador();
    var inexistente = clienteSinSesion().iniciarSesion(
      "nadie@example.test",
      CONTRASENA_DE_CUENTA
    );
    clienteAdministrador().enviar(
      "PATCH",
      "usuarios/" + cuenta.id() + "/estado",
      "{\"estado\":\"Inactivo\"}"
    );

    var inactivo = clienteSinSesion().iniciarSesion(cuenta.correo(), CONTRASENA_DE_CUENTA);

    assertThat(inactivo.statusCode()).isEqualTo(401);
    assertThat(inactivo.body()).isEqualTo(inexistente.body());
  }

  @Test
  @DisplayName("CU01-EX4 El bloqueo de un correo no afecta a otro")
  void elBloqueoDeUnCorreoNoAfectaAOtro() throws Exception {
    var bloqueada = crearCoordinador();
    var otra = crearCoordinador();
    fallarIntentos(bloqueada.correo(), 5);

    assertThat(
      clienteSinSesion().iniciarSesion(bloqueada.correo(), CONTRASENA_DE_CUENTA).statusCode()
    ).isEqualTo(429);
    assertThat(
      clienteSinSesion().iniciarSesion(otra.correo(), CONTRASENA_DE_CUENTA).statusCode()
    ).isEqualTo(200);
  }

  @Test
  @DisplayName("CU01-EX5 Un cuerpo inválido responde 400 sin contar como intento")
  void unCuerpoInvalidoResponde400() throws Exception {
    var cliente = clienteSinSesion();
    cliente.get("sesion/perfil");
    for (String cuerpo : new String[] {
      "{\"correo\":\"\",\"contrasena\":\"x\"}",
      "{\"correo\":\"no-es-correo\",\"contrasena\":\"x\"}",
      "{\"correo\":\"a@example.test\",\"contrasena\":\"\"}",
    }) assertThat(cliente.enviar("POST", "sesion/iniciar", cuerpo).statusCode())
      .as(cuerpo)
      .isEqualTo(400);
  }

  @Test
  @DisplayName("CU01-EX8 La sesión dura 30 minutos y una cookie desconocida responde 401")
  void laSesionExpiraYUnaCookieDesconocidaNoEntra() throws Exception {
    assertThat(duracionDeSesion).isEqualTo(Duration.ofMinutes(30));

    var peticion = HttpRequest.newBuilder(
      URI.create("http://localhost:" + puerto + "/api/sesion/perfil")
    )
      .header("Cookie", "HUSI_SESION=sesion-inventada-o-vencida")
      .GET()
      .build();
    var respuesta = HttpClient.newHttpClient().send(peticion, HttpResponse.BodyHandlers.ofString());

    assertThat(respuesta.statusCode()).isEqualTo(401);
    assertThat(respuesta.body()).contains("Tu sesión expiró");
  }

  @Test
  @DisplayName("CU01-EX9 La cookie es HttpOnly y SameSite=Strict, y su id cambia al ingresar")
  void laCookieEstaProtegidaYSuIdCambiaAlIngresar() throws Exception {
    var cuenta = crearCoordinador();
    var cliente = clienteSinSesion();

    String primeraCookie = cookieDeSesion(
      cliente.iniciarSesion(cuenta.correo(), CONTRASENA_DE_CUENTA)
    );
    String segundaCookie = cookieDeSesion(
      cliente.iniciarSesion(cuenta.correo(), CONTRASENA_DE_CUENTA)
    );

    assertThat(primeraCookie).containsIgnoringCase("HttpOnly").containsIgnoringCase("SameSite=Strict");
    assertThat(segundaCookie.split(";")[0]).isNotEqualTo(primeraCookie.split(";")[0]);
  }
}
