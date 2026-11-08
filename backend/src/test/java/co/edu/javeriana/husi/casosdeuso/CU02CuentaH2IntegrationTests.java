package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.ClienteApiDePrueba;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-02 Modificar los datos de la cuenta (RF-06). */
class CU02CuentaH2IntegrationTests extends BaseCasoDeUsoPorApi {

  private static final String CONTRASENA_NUEVA = "Contrasena-Nueva-2";

  private HttpResponse<String> cambiarCredenciales(ClienteApiDePrueba cliente, String camposNuevos)
    throws Exception {
    return cliente.enviar(
      "PATCH",
      "sesion/credenciales",
      "{\"contrasenaActual\":\"" + CONTRASENA_DE_CUENTA + "\"" + camposNuevos + "}"
    );
  }

  @Test
  @DisplayName("CU02-FA1 Cambia solo el correo y la contraseña sigue igual")
  void cambiaSoloElCorreo() throws Exception {
    var cuenta = crearCoordinador();
    String correoNuevo = "nuevo-" + cuenta.correo();

    var cambio = cambiarCredenciales(clienteDe(cuenta), ",\"correo\":\"" + correoNuevo + "\"");

    assertThat(cambio.statusCode()).as(cambio.body()).isEqualTo(204);
    assertThat(
      clienteSinSesion().iniciarSesion(correoNuevo, CONTRASENA_DE_CUENTA).statusCode()
    ).isEqualTo(200);
  }

  @Test
  @DisplayName("CU02-FA1 Cambia solo la contraseña y el correo sigue igual")
  void cambiaSoloLaContrasena() throws Exception {
    var cuenta = crearCoordinador();

    var cambio = cambiarCredenciales(
      clienteDe(cuenta),
      ",\"contrasenaNueva\":\"" + CONTRASENA_NUEVA + "\""
    );

    assertThat(cambio.statusCode()).as(cambio.body()).isEqualTo(204);
    assertThat(
      clienteSinSesion().iniciarSesion(cuenta.correo(), CONTRASENA_NUEVA).statusCode()
    ).isEqualTo(200);
  }

  @Test
  @DisplayName("CU02-EX2 Los fallos de la contraseña actual suman al bloqueo por intentos")
  void losFallosDeLaContrasenaActualSumanAlBloqueo() throws Exception {
    var cuenta = crearCoordinador();
    var cliente = clienteDe(cuenta);
    String cuerpo =
      "{\"contrasenaActual\":\"No-Es-La-Clave-9\",\"contrasenaNueva\":\"" + CONTRASENA_NUEVA + "\"}";
    for (int intento = 1; intento <= 5; intento++) assertThat(
      cliente.enviar("PATCH", "sesion/credenciales", cuerpo).statusCode()
    ).isEqualTo(400);

    assertThat(cliente.enviar("PATCH", "sesion/credenciales", cuerpo).statusCode()).isEqualTo(429);
    assertThat(
      clienteSinSesion().iniciarSesion(cuenta.correo(), CONTRASENA_DE_CUENTA).statusCode()
    ).isEqualTo(429);
  }

  @Test
  @DisplayName("CU02-EX3 Un correo que ya usa otra cuenta responde 409")
  void unCorreoUsadoPorOtraCuentaResponde409() throws Exception {
    var cuenta = crearCoordinador();
    var otra = crearCoordinador();

    var cambio = cambiarCredenciales(clienteDe(cuenta), ",\"correo\":\"" + otra.correo() + "\"");

    assertThat(cambio.statusCode()).isEqualTo(409);
    assertThat(cambio.body()).contains("Ya existe un usuario con ese correo");
  }

  @Test
  @DisplayName("CU02-EX4 Un correo vacío o inválido responde 400")
  void unCorreoInvalidoResponde400() throws Exception {
    var cliente = clienteDe(crearCoordinador());
    for (String correo : new String[] { "", "no-es-correo" }) assertThat(
      cambiarCredenciales(cliente, ",\"correo\":\"" + correo + "\"").statusCode()
    )
      .as(correo)
      .isEqualTo(400);
  }

  @Test
  @DisplayName("CU02-EX5 Una contraseña de menos de 8 caracteres o más de 72 bytes responde 400")
  void unaContrasenaFueraDeLimitesResponde400() throws Exception {
    var cliente = clienteDe(crearCoordinador());
    // 40 caracteres de 2 bytes: caben en los 72 caracteres del campo, pero son 80 bytes.
    String masDe72Bytes = "ñ".repeat(40);
    for (String contrasena : new String[] { "Corta-1", masDe72Bytes }) {
      var cambio = cambiarCredenciales(cliente, ",\"contrasenaNueva\":\"" + contrasena + "\"");
      assertThat(cambio.statusCode()).as(contrasena).isEqualTo(400);
      assertThat(cambio.body()).contains("al menos 8 caracteres");
    }
  }

  @Test
  @DisplayName("CU02-EX6 Una contraseña nueva igual a la actual responde 400")
  void unaContrasenaIgualALaActualResponde400() throws Exception {
    var cambio = cambiarCredenciales(
      clienteDe(crearCoordinador()),
      ",\"contrasenaNueva\":\"" + CONTRASENA_DE_CUENTA + "\""
    );
    assertThat(cambio.statusCode()).isEqualTo(400);
    assertThat(cambio.body()).contains("distinta de la actual");
  }

  @Test
  @DisplayName("CU02-EX7 Una petición sin datos o con nombres en blanco responde 400")
  void unaPeticionSinDatosResponde400() throws Exception {
    var cliente = clienteDe(crearCoordinador());

    assertThat(cambiarCredenciales(cliente, "").statusCode()).isEqualTo(400);
    assertThat(cliente.enviar("PATCH", "sesion/perfil", "{}").statusCode()).isEqualTo(400);
    assertThat(
      cliente.enviar("PATCH", "sesion/perfil", "{\"nombres\":\"   \"}").statusCode()
    ).isEqualTo(400);
  }

  @Test
  @DisplayName("CU02-EX9 El historial deja «Se cambió» y nunca la contraseña ni su hash")
  void elHistorialNoGuardaLaContrasena() throws Exception {
    var cuenta = crearCoordinador();

    cambiarCredenciales(clienteDe(cuenta), ",\"contrasenaNueva\":\"" + CONTRASENA_NUEVA + "\"");

    var movimientos = jdbc.queryForList(
      "select coalesce(datos_anteriores,'') as antes,datos_nuevos as despues,detalle from historial_auditoria where correo_responsable=?",
      cuenta.correo()
    );
    assertThat(movimientos).hasSize(1);
    String movimiento = movimientos.getFirst().toString();
    assertThat(movimiento)
      .contains("Se cambió")
      .doesNotContain(CONTRASENA_NUEVA)
      .doesNotContain(CONTRASENA_DE_CUENTA)
      .doesNotContain("$2a$");
  }
}
