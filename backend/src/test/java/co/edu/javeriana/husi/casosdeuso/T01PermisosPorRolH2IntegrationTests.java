package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.ClienteApiDePrueba;
import java.net.http.HttpResponse;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * T-01 y T-02: cada endpoint frente a cada perfil (RF-03, RNF-07, RNF-08). Los ids no existen y los
 * cuerpos están vacíos a propósito: lo que se comprueba es quién pasa la seguridad, sin modificar
 * ningún dato.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class T01PermisosPorRolH2IntegrationTests extends BaseCasoDeUsoPorApi {

  private static final String ROTACION = "rotaciones/gestion/999999/2099-01-01/2099-01-31";

  private ClienteApiDePrueba administrador;
  private ClienteApiDePrueba coordinador;

  private ClienteApiDePrueba administrador() throws Exception {
    if (administrador == null) administrador = clienteAdministrador();
    return administrador;
  }

  private ClienteApiDePrueba coordinador() throws Exception {
    if (coordinador == null) coordinador = clienteDe(crearCoordinador());
    return coordinador;
  }

  private HttpResponse<String> llamar(ClienteApiDePrueba cliente, String metodo, String ruta)
    throws Exception {
    // Primero una lectura: entrega la cookie XSRF-TOKEN, sin la cual el rechazo sería por CSRF.
    cliente.get("sesion/perfil");
    return "GET".equals(metodo) ? cliente.get(ruta) : cliente.enviar(metodo, ruta, "{}");
  }

  /** Todas las rutas exclusivas del administrador, una por cada endpoint de los controllers. */
  static Stream<Arguments> rutasDeAdministrador() {
    return Stream.of(
      Arguments.of("GET", "universidades"),
      Arguments.of("GET", "universidades/999999"),
      Arguments.of("POST", "universidades"),
      Arguments.of("PUT", "universidades/999999"),
      Arguments.of("DELETE", "universidades/999999"),
      Arguments.of("PATCH", "universidades/999999/estado"),
      Arguments.of("DELETE", "universidades/999999/definitivo"),
      Arguments.of("GET", "universidades/999999/programas"),
      Arguments.of("GET", "universidades/buscar"),
      Arguments.of("GET", "programas"),
      Arguments.of("GET", "programas/999999"),
      Arguments.of("GET", "programas/buscar"),
      Arguments.of("POST", "programas"),
      Arguments.of("PUT", "programas/999999"),
      Arguments.of("DELETE", "programas/999999"),
      Arguments.of("PATCH", "programas/999999/estado"),
      Arguments.of("DELETE", "programas/999999/definitivo"),
      Arguments.of("POST", "programas/importacion/validar"),
      Arguments.of("POST", "programas/importacion"),
      Arguments.of("GET", "servicios"),
      Arguments.of("GET", "servicios/catalogo"),
      Arguments.of("GET", "servicios/999999"),
      Arguments.of("POST", "servicios"),
      Arguments.of("PUT", "servicios/999999"),
      Arguments.of("PATCH", "servicios/999999/estado"),
      Arguments.of("DELETE", "servicios/999999/definitivo"),
      Arguments.of("GET", "asignaturas"),
      Arguments.of("GET", "asignaturas/catalogo"),
      Arguments.of("GET", "asignaturas/999999"),
      Arguments.of("POST", "asignaturas"),
      Arguments.of("PUT", "asignaturas/999999"),
      Arguments.of("PATCH", "asignaturas/999999/estado"),
      Arguments.of("DELETE", "asignaturas/999999/definitivo"),
      Arguments.of("GET", "residentes"),
      Arguments.of("GET", "residentes/999999"),
      Arguments.of("POST", "residentes"),
      Arguments.of("POST", "residentes/buscar-por-documentos"),
      Arguments.of("PUT", "residentes/999999"),
      Arguments.of("PATCH", "residentes/999999/estado"),
      Arguments.of("DELETE", "residentes/999999/definitivo"),
      Arguments.of("GET", "usuarios"),
      Arguments.of("GET", "usuarios/999999"),
      Arguments.of("POST", "usuarios"),
      Arguments.of("PUT", "usuarios/999999"),
      Arguments.of("PATCH", "usuarios/999999/estado"),
      Arguments.of("DELETE", "usuarios/999999/definitivo"),
      Arguments.of("GET", "rotaciones"),
      Arguments.of("GET", "rotaciones/gestion"),
      Arguments.of("GET", "rotaciones/gestion/catalogos"),
      Arguments.of("POST", "rotaciones/gestion"),
      Arguments.of("POST", "rotaciones/gestion/importar/validar"),
      Arguments.of("POST", "rotaciones/gestion/importar"),
      Arguments.of("POST", "rotaciones/carga-inicial/validar"),
      Arguments.of("POST", "rotaciones/carga-inicial/confirmar"),
      Arguments.of("GET", ROTACION),
      Arguments.of("PUT", ROTACION),
      Arguments.of("PATCH", ROTACION + "/estado"),
      Arguments.of("DELETE", ROTACION),
      Arguments.of("GET", ROTACION + "/residentes"),
      Arguments.of("GET", ROTACION + "/candidatos"),
      Arguments.of("POST", ROTACION + "/residentes"),
      Arguments.of("PATCH", ROTACION + "/residentes/999999/estado"),
      Arguments.of("DELETE", ROTACION + "/residentes/999999"),
      Arguments.of("GET", ROTACION + "/espera"),
      Arguments.of("POST", ROTACION + "/espera"),
      Arguments.of("GET", "dashboard"),
      Arguments.of("GET", "dashboard/relaciones"),
      Arguments.of("GET", "historial"),
      Arguments.of("GET", "historial/999999"),
      Arguments.of("POST", "historial/importaciones/no-es-un-lote/cancelar"),
      Arguments.of("GET", "catalogos"),
      Arguments.of("GET", "roles"),
      Arguments.of("GET", "tipos-documento"),
      Arguments.of("GET", "perfiles-academicos")
    );
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("rutasDeAdministrador")
  @DisplayName("T-01 Sin sesión, las rutas de administrador responden 401")
  void sinSesionResponden401(String metodo, String ruta) throws Exception {
    assertThat(llamar(clienteSinSesion(), metodo, ruta).statusCode()).isEqualTo(401);
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("rutasDeAdministrador")
  @DisplayName("T-02 El coordinador recibe 403 en todas las rutas de administrador")
  void elCoordinadorRecibe403(String metodo, String ruta) throws Exception {
    assertThat(llamar(coordinador(), metodo, ruta).statusCode()).isEqualTo(403);
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("rutasDeAdministrador")
  @DisplayName("T-01 El administrador pasa la seguridad en todas sus rutas")
  void elAdministradorPasaLaSeguridad(String metodo, String ruta) throws Exception {
    assertThat(llamar(administrador(), metodo, ruta).statusCode()).isNotIn(401, 403, 500);
  }

  static Stream<String> rutasDelCoordinador() {
    return Stream.of("coordinador/residentes", "coordinador/dashboard");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rutasDelCoordinador")
  @DisplayName("T-01 Las rutas del coordinador solo admiten su rol")
  void lasRutasDelCoordinadorSoloAdmitenSuRol(String ruta) throws Exception {
    assertThat(clienteSinSesion().get(ruta).statusCode()).isEqualTo(401);
    assertThat(administrador().get(ruta).statusCode()).isEqualTo(403);
    assertThat(coordinador().get(ruta).statusCode()).isEqualTo(200);
  }

  static Stream<Arguments> rutasDeCualquierPerfil() {
    return Stream.of(
      Arguments.of("GET", "sesion/perfil"),
      Arguments.of("PATCH", "sesion/perfil"),
      Arguments.of("PATCH", "sesion/credenciales")
    );
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("rutasDeCualquierPerfil")
  @DisplayName("T-01 Las rutas de la cuenta propia solo exigen sesión")
  void lasRutasDeLaCuentaPropiaSoloExigenSesion(String metodo, String ruta) throws Exception {
    assertThat(llamar(clienteSinSesion(), metodo, ruta).statusCode()).isEqualTo(401);
    assertThat(llamar(administrador(), metodo, ruta).statusCode()).isNotIn(401, 403);
    assertThat(llamar(coordinador(), metodo, ruta).statusCode()).isNotIn(401, 403);
  }
}
