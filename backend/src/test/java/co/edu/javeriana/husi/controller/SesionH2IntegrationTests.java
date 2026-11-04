package co.edu.javeriana.husi.controller;

import static co.edu.javeriana.husi.ClienteApiDePrueba.CONTRASENA_ADMINISTRADOR;
import static co.edu.javeriana.husi.ClienteApiDePrueba.CORREO_ADMINISTRADOR;
import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.ClienteApiDePrueba;
import co.edu.javeriana.husi.util.Calendario;
import java.time.LocalDate;
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
    "spring.datasource.url=jdbc:h2:mem:sesion;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;OPTIMIZE_REUSE_RESULTS=0;DB_CLOSE_DELAY=-1",
    "husi.seguridad.administrador-inicial.contrasena=" + CONTRASENA_ADMINISTRADOR,
  }
)
@ActiveProfiles("local")
class SesionH2IntegrationTests {

  private static final String CONTRASENA_COORDINADOR = "Coordinador-Prueba-1";
  private static final int ID_ROL_ADMINISTRADOR = 1;
  private static final int ID_ROL_COORDINADOR = 2;
  private static final String CONTRASENA_ADMINISTRADOR_SECUNDARIO = "Administrador-Prueba-2";

  @LocalServerPort
  int puerto;

  @Autowired
  JdbcTemplate jdbc;

  /**
   * Crea un coordinador (con el servicio que coordina, obligatorio para ese rol) usando la sesión
   * del administrador y devuelve su correo.
   */
  private String crearCoordinador() throws Exception {
    return crearCoordinadorDelServicio(crearServicio("Servicio " + UUID.randomUUID()));
  }

  /** Crea un servicio activo con cupo para tres residentes simultáneos y devuelve su id. */
  private String crearServicio(String nombre) throws Exception {
    var servicio = ClienteApiDePrueba.conSesionDeAdministrador(puerto).enviar(
      "POST",
      "servicios",
      "{\"nombre\":\"" + nombre + "\",\"cupoSimultaneo\":3,\"cupoMaximoInstalado\":3}"
    );
    assertThat(servicio.statusCode()).as(servicio.body()).isEqualTo(201);
    return servicio.body().replaceAll("^\\{\"id\":(\\d+).*", "$1");
  }

  private String crearCoordinadorDelServicio(String idServicio) throws Exception {
    var administrador = ClienteApiDePrueba.conSesionDeAdministrador(puerto);
    String correo = "coordinador-" + UUID.randomUUID() + "@example.test";
    var respuesta = administrador.enviar(
      "POST",
      "usuarios",
      "{\"nombre\":\"Coordinador\",\"apellido\":\"Prueba\",\"correo\":\"" +
        correo +
        "\",\"rolId\":" +
        ID_ROL_COORDINADOR +
        ",\"contrasena\":\"" +
        CONTRASENA_COORDINADOR +
        "\",\"servicioId\":" +
        idServicio +
        "}"
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

  @Test
  void ajustesActualizaSoloLosNombresEnviados() throws Exception {
    String correo = crearCoordinador();
    var coordinador = new ClienteApiDePrueba(puerto);
    coordinador.iniciarSesion(correo, CONTRASENA_COORDINADOR);

    var respuesta = coordinador.enviar("PATCH", "sesion/perfil", "{\"nombres\":\"  Ana María \"}");

    assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(200);
    assertThat(respuesta.body())
      .contains("\"nombres\":\"Ana María\"")
      .contains("\"apellidos\":\"Prueba\"");
    assertThat(
      coordinador.enviar("PATCH", "sesion/perfil", "{\"apellidos\":\"   \"}").statusCode()
    ).isEqualTo(400);
    assertThat(coordinador.enviar("PATCH", "sesion/perfil", "{}").statusCode()).isEqualTo(400);
  }

  @Test
  void ajustesCambiaCorreoYContrasenaConLaContrasenaActual() throws Exception {
    String correo = crearCoordinador();
    String correoNuevo = "nuevo-" + correo;
    var coordinador = new ClienteApiDePrueba(puerto);
    coordinador.iniciarSesion(correo, CONTRASENA_COORDINADOR);

    var contrasenaErrada = coordinador.enviar(
      "PATCH",
      "sesion/credenciales",
      "{\"contrasenaActual\":\"no-es-esta-1\",\"correo\":\"" + correoNuevo + "\"}"
    );
    assertThat(contrasenaErrada.statusCode()).isEqualTo(400);
    assertThat(contrasenaErrada.body()).contains("contraseña actual no es correcta");

    var respuesta = coordinador.enviar(
      "PATCH",
      "sesion/credenciales",
      "{\"contrasenaActual\":\"" +
        CONTRASENA_COORDINADOR +
        "\",\"correo\":\"" +
        correoNuevo.toUpperCase() +
        "\",\"contrasenaNueva\":\"Otra-Clave-2\"}"
    );
    assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(204);
    assertThat(coordinador.get("sesion/perfil").statusCode()).isEqualTo(401);

    var sesionNueva = new ClienteApiDePrueba(puerto);
    assertThat(sesionNueva.iniciarSesion(correo, CONTRASENA_COORDINADOR).statusCode()).isEqualTo(401);
    assertThat(sesionNueva.iniciarSesion(correoNuevo, "Otra-Clave-2").statusCode()).isEqualTo(200);
    assertThat(
      jdbc.queryForObject(
        "select count(*) from historial_auditoria where datos_nuevos like '%Se cambió%' and datos_nuevos like ?",
        Integer.class,
        "%" + correoNuevo + "%"
      )
    ).isEqualTo(1);
  }

  @Test
  void elCoordinadorVeLosResidentesQueRotanHoyYLosProximosDeSuServicio() throws Exception {
    String nombreServicio = "Servicio " + UUID.randomUUID();
    String idServicio = crearServicio(nombreServicio);
    String correo = crearCoordinadorDelServicio(idServicio);
    LocalDate hoy = Calendario.hoy();
    int asignatura = crearAsignatura(Integer.parseInt(idServicio));
    int asignaturaDeOtroServicio = crearAsignatura(
      Integer.parseInt(crearServicio("Servicio " + UUID.randomUUID()))
    );

    asignar("Residente En Curso", asignatura, hoy.minusDays(5), hoy.plusDays(5));
    asignar("Residente Finalizado", asignatura, hoy.minusDays(30), hoy.minusDays(1));
    asignar("Residente Lejano", asignatura, hoy.plusDays(30), hoy.plusDays(40));
    asignar("Residente Futuro", asignatura, hoy.plusDays(1), hoy.plusDays(20));
    asignar("Residente De Otro Servicio", asignaturaDeOtroServicio, hoy, hoy.plusDays(5));

    var coordinador = new ClienteApiDePrueba(puerto);
    coordinador.iniciarSesion(correo, CONTRASENA_COORDINADOR);
    var respuesta = coordinador.get("coordinador/residentes");

    assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(200);
    String cuerpo = respuesta.body();
    int inicioProximos = cuerpo.indexOf("\"proximos\":");
    assertThat(inicioProximos).as(cuerpo).isPositive();
    String enCurso = cuerpo.substring(0, inicioProximos);
    String proximos = cuerpo.substring(inicioProximos);
    assertThat(enCurso)
      .contains("\"servicio\":\"" + nombreServicio + "\"")
      .contains("Residente En Curso")
      .contains("Universidad de demostración")
      .doesNotContain("Residente Futuro")
      .doesNotContain("Residente Lejano");
    assertThat(proximos).contains("Residente Futuro").doesNotContain("Residente En Curso");
    assertThat(proximos.indexOf("Residente Futuro"))
      .as("las próximas se ordenan por fecha de inicio")
      .isLessThan(proximos.indexOf("Residente Lejano"));
    assertThat(cuerpo)
      .doesNotContain("Residente Finalizado")
      .doesNotContain("Residente De Otro Servicio");
    assertThat(
      ClienteApiDePrueba.conSesionDeAdministrador(puerto).get("coordinador/residentes").statusCode()
    ).isEqualTo(403);
  }

  private int crearAsignatura(int idServicio) {
    String nombre = "Asignatura " + UUID.randomUUID();
    jdbc.update(
      "insert into asignatura(id_servicio,nombre_asignatura) values (?,?)",
      idServicio,
      nombre
    );
    return jdbc.queryForObject(
      "select id_asignatura from asignatura where nombre_asignatura=?",
      Integer.class,
      nombre
    );
  }

  /** Crea la rotación y un residente asignado a ella (el trigger le descuenta el cupo). */
  private void asignar(String nombre, int asignatura, LocalDate inicio, LocalDate fin) {
    jdbc.update(
      "insert into rotacion(id_asignatura,fecha_inicio,fecha_fin) values (?,?,?)",
      asignatura,
      inicio,
      fin
    );
    String documento = UUID.randomUUID().toString().substring(0, 20);
    jdbc.update(
      "insert into residente(id_programa,id_tipo_documento,nombre_completo,numero_documento) values (1,1,?,?)",
      nombre,
      documento
    );
    jdbc.update(
      "insert into rotacion_residente(id_residente,id_asignatura,fecha_inicio,fecha_fin) values ((select id_residente from residente where numero_documento=?),?,?,?)",
      documento,
      asignatura,
      inicio,
      fin
    );
  }

  // ---------- Sesión vigente: la cuenta cambió mientras la sesión seguía abierta ----------

  /** Otro administrador con sesión iniciada; {@code id} es el de su cuenta. */
  private record AdministradorConSesion(String id, String correo, ClienteApiDePrueba sesion) {}

  private AdministradorConSesion crearAdministradorConSesion() throws Exception {
    String correo = "administrador-" + UUID.randomUUID() + "@example.test";
    var respuesta = ClienteApiDePrueba.conSesionDeAdministrador(puerto).enviar(
      "POST",
      "usuarios",
      datosDeCuenta(correo, ID_ROL_ADMINISTRADOR, null, CONTRASENA_ADMINISTRADOR_SECUNDARIO)
    );
    assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(201);
    String id = String.valueOf(
      jdbc.queryForObject("select id_usuario from usuario where correo=?", Integer.class, correo)
    );
    var sesion = new ClienteApiDePrueba(puerto);
    assertThat(
      sesion.iniciarSesion(correo, CONTRASENA_ADMINISTRADOR_SECUNDARIO).statusCode()
    ).isEqualTo(200);
    assertThat(sesion.get("usuarios").statusCode()).isEqualTo(200);
    return new AdministradorConSesion(id, correo, sesion);
  }

  /** Cuerpo de POST y PUT /usuarios; al editar, sin contraseña se conserva la actual. */
  private String datosDeCuenta(String correo, int rolId, String idServicio, String contrasena) {
    return (
      "{\"nombre\":\"Segundo\",\"apellido\":\"Administrador\",\"correo\":\"" +
      correo +
      "\",\"rolId\":" +
      rolId +
      (idServicio == null ? "" : ",\"servicioId\":" + idServicio) +
      (contrasena == null ? "" : ",\"contrasena\":\"" + contrasena + "\"") +
      "}"
    );
  }

  @Test
  void cambiarLasCredencialesCierraTambienLasOtrasSesionesDeLaCuenta() throws Exception {
    String correo = crearCoordinador();
    var sesionQueCambia = new ClienteApiDePrueba(puerto);
    var otraSesion = new ClienteApiDePrueba(puerto);
    sesionQueCambia.iniciarSesion(correo, CONTRASENA_COORDINADOR);
    otraSesion.iniciarSesion(correo, CONTRASENA_COORDINADOR);

    var cambio = sesionQueCambia.enviar(
      "PATCH",
      "sesion/credenciales",
      "{\"contrasenaActual\":\"" +
        CONTRASENA_COORDINADOR +
        "\",\"contrasenaNueva\":\"Otra-Clave-3\"}"
    );

    assertThat(cambio.statusCode()).as(cambio.body()).isEqualTo(204);
    assertThat(otraSesion.get("sesion/perfil").statusCode()).isEqualTo(401);
  }

  @Test
  void cambiarDatosQueNoDanAccesoNoTerminaLaSesion() throws Exception {
    var cuenta = crearAdministradorConSesion();
    var edicion = ClienteApiDePrueba.conSesionDeAdministrador(puerto).enviar(
      "PUT",
      "usuarios/" + cuenta.id(),
      datosDeCuenta(cuenta.correo(), ID_ROL_ADMINISTRADOR, null, null).replace("Segundo", "Otro")
    );
    assertThat(edicion.statusCode()).as(edicion.body()).isEqualTo(200);
    assertThat(cuenta.sesion().get("usuarios").statusCode()).isEqualTo(200);
  }

  @Test
  void desactivarUnaCuentaTerminaSuSesionAbierta() throws Exception {
    var cuenta = crearAdministradorConSesion();
    var desactivacion = ClienteApiDePrueba.conSesionDeAdministrador(puerto).enviar(
      "PATCH",
      "usuarios/" + cuenta.id() + "/estado",
      "{\"estado\":\"Inactivo\"}"
    );
    assertThat(desactivacion.statusCode()).as(desactivacion.body()).isEqualTo(200);

    assertThat(cuenta.sesion().get("usuarios").statusCode()).isEqualTo(401);
    assertThat(
      new ClienteApiDePrueba(puerto)
        .iniciarSesion(cuenta.correo(), CONTRASENA_ADMINISTRADOR_SECUNDARIO)
        .statusCode()
    ).isEqualTo(401);
  }

  @Test
  void quitarElRolDeAdministradorTerminaSuSesionYElNuevoRolNoTieneEsosPermisos() throws Exception {
    var cuenta = crearAdministradorConSesion();
    var cambioDeRol = ClienteApiDePrueba.conSesionDeAdministrador(puerto).enviar(
      "PUT",
      "usuarios/" + cuenta.id(),
      datosDeCuenta(
        cuenta.correo(),
        ID_ROL_COORDINADOR,
        crearServicio("Servicio " + UUID.randomUUID()),
        null
      )
    );
    assertThat(cambioDeRol.statusCode()).as(cambioDeRol.body()).isEqualTo(200);

    assertThat(cuenta.sesion().get("usuarios").statusCode()).isEqualTo(401);

    var nuevaSesion = new ClienteApiDePrueba(puerto);
    nuevaSesion.iniciarSesion(cuenta.correo(), CONTRASENA_ADMINISTRADOR_SECUNDARIO);
    assertThat(nuevaSesion.get("usuarios").statusCode()).isEqualTo(403);
    assertThat(nuevaSesion.get("coordinador/residentes").statusCode()).isEqualTo(200);
  }

  @Test
  void conUnaSesionTerminadaSePuedeVolverAIngresar() throws Exception {
    var cuenta = crearAdministradorConSesion();
    String contrasenaNueva = "Nueva-Clave-4";
    var cambio = ClienteApiDePrueba.conSesionDeAdministrador(puerto).enviar(
      "PUT",
      "usuarios/" + cuenta.id(),
      datosDeCuenta(cuenta.correo(), ID_ROL_ADMINISTRADOR, null, contrasenaNueva)
    );
    assertThat(cambio.statusCode()).as(cambio.body()).isEqualTo(200);

    // Inicia sesión llevando todavía la cookie de la sesión que ya no es válida.
    assertThat(
      cuenta.sesion().iniciarSesion(cuenta.correo(), contrasenaNueva).statusCode()
    ).isEqualTo(200);
    assertThat(cuenta.sesion().get("usuarios").statusCode()).isEqualTo(200);
  }

  @Test
  void eliminarUnaCuentaTerminaSuSesionYConservaSusMovimientos() throws Exception {
    var cuenta = crearAdministradorConSesion();
    String nombreServicio = "Servicio-" + UUID.randomUUID();
    var creacion = cuenta.sesion().enviar(
      "POST",
      "servicios",
      "{\"nombre\":\"" + nombreServicio + "\",\"cupoSimultaneo\":1,\"cupoMaximoInstalado\":1}"
    );
    assertThat(creacion.statusCode()).as(creacion.body()).isEqualTo(201);

    var administrador = ClienteApiDePrueba.conSesionDeAdministrador(puerto);
    var eliminacion = administrador.enviar(
      "DELETE",
      "usuarios/" + cuenta.id() + "/definitivo",
      ""
    );
    assertThat(eliminacion.statusCode()).as(eliminacion.body()).isEqualTo(204);

    assertThat(cuenta.sesion().get("usuarios").statusCode()).isEqualTo(401);

    var listado = administrador.get("historial?q=" + nombreServicio);
    assertThat(listado.statusCode()).as(listado.body()).isEqualTo(200);
    assertThat(listado.body()).contains("Segundo Administrador");

    Integer idMovimiento = jdbc.queryForObject(
      "select id_historial from historial_auditoria where registro=?",
      Integer.class,
      nombreServicio
    );
    var detalle = administrador.get("historial/" + idMovimiento);
    assertThat(detalle.statusCode()).as(detalle.body()).isEqualTo(200);
    assertThat(detalle.body()).contains("Segundo Administrador", cuenta.correo());
  }

  @Test
  void noSeEliminaUnaCuentaQueCoordinaUnServicio() throws Exception {
    String correo = crearCoordinador();
    Integer id = jdbc.queryForObject(
      "select id_usuario from usuario where correo=?",
      Integer.class,
      correo
    );
    var eliminacion = ClienteApiDePrueba.conSesionDeAdministrador(puerto).enviar(
      "DELETE",
      "usuarios/" + id + "/definitivo",
      ""
    );
    assertThat(eliminacion.statusCode()).isEqualTo(409);
    assertThat(eliminacion.body()).contains("coordina el servicio");
  }
}
