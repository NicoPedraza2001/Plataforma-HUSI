package co.edu.javeriana.husi;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Cliente HTTP para los tests que llaman a la API por red. Guarda las cookies como un navegador
 * (sesión {@code HUSI_SESION} y {@code XSRF-TOKEN}) y, como Angular, copia el token CSRF en la
 * cabecera {@code X-XSRF-TOKEN} de cada petición.
 */
public class ClienteApiDePrueba {

  public static final String CORREO_ADMINISTRADOR = "admin@husi.com";
  /** Solo existe en la base H2 en memoria de los tests (ver husi.seguridad.administrador-inicial). */
  public static final String CONTRASENA_ADMINISTRADOR = "ContrasenaDePrueba-123";

  private final String urlBase;
  private final CookieManager cookies = new CookieManager();
  private final HttpClient http;

  public ClienteApiDePrueba(int puerto) {
    this.urlBase = "http://localhost:" + puerto + "/api/";
    this.http = HttpClient.newBuilder().cookieHandler(cookies).build();
  }

  /** Cliente con la sesión del administrador ya iniciada. */
  public static ClienteApiDePrueba conSesionDeAdministrador(int puerto) throws Exception {
    var cliente = new ClienteApiDePrueba(puerto);
    var respuesta = cliente.iniciarSesion(CORREO_ADMINISTRADOR, CONTRASENA_ADMINISTRADOR);
    if (respuesta.statusCode() != 200) throw new IllegalStateException(respuesta.body());
    return cliente;
  }

  public HttpResponse<String> iniciarSesion(String correo, String contrasena) throws Exception {
    // La primera consulta entrega la cookie XSRF-TOKEN, igual que en el navegador.
    if (tokenCsrf() == null) get("sesion/perfil");
    return enviar(
      "POST",
      "sesion/iniciar",
      "{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}"
    );
  }

  public HttpResponse<String> get(String ruta) throws Exception {
    return http.send(
      HttpRequest.newBuilder(URI.create(urlBase + ruta)).GET().build(),
      HttpResponse.BodyHandlers.ofString()
    );
  }

  public HttpResponse<String> enviar(String metodo, String ruta, String cuerpo) throws Exception {
    var peticion = HttpRequest.newBuilder(URI.create(urlBase + ruta))
      .header("Content-Type", "application/json")
      .method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo));
    String token = tokenCsrf();
    if (token != null) peticion.header("X-XSRF-TOKEN", token);
    return http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
  }

  /** Igual que {@link #enviar}, pero sin la cabecera CSRF (simula un sitio malicioso). */
  public HttpResponse<String> enviarSinTokenCsrf(String metodo, String ruta, String cuerpo)
    throws Exception {
    return http.send(
      HttpRequest.newBuilder(URI.create(urlBase + ruta))
        .header("Content-Type", "application/json")
        .method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo))
        .build(),
      HttpResponse.BodyHandlers.ofString()
    );
  }

  private String tokenCsrf() {
    return cookies
      .getCookieStore()
      .getCookies()
      .stream()
      .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
      .map(HttpCookie::getValue)
      .findFirst()
      .orElse(null);
  }
}
