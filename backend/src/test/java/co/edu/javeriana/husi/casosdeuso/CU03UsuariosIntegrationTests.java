package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

/** CU-03 Gestionar usuarios y coordinadores (RF-04, RF-05). */
class CU03UsuariosIntegrationTests extends BaseCasoDeUso {

  @Autowired
  JsonMapper jsonMapper;

  private String estadoDeLaCoordinacion(Integer usuarioId) {
    return jdbc.queryForObject(
      "select estado from coordinador where id_usuario=?",
      String.class,
      usuarioId
    );
  }

  @Test
  @DisplayName("CU03-FP3 Desactivar y reactivar al usuario copia el estado a su coordinación")
  void elEstadoDelUsuarioSeCopiaASuCoordinacion() {
    var coordinador = coordinadorDe(servicioConCupo(2).id());

    assertThat(usuarios.cambiarEstado(coordinador.id(), "Inactivo").estado()).isEqualTo("Inactivo");
    assertThat(estadoDeLaCoordinacion(coordinador.id())).isEqualTo("Inactivo");

    assertThat(usuarios.cambiarEstado(coordinador.id(), "Activo").estado()).isEqualTo("Activo");
    assertThat(estadoDeLaCoordinacion(coordinador.id())).isEqualTo("Activo");
  }

  @Test
  @DisplayName("CU03-FA1 Al cambiar de servicio, el panel del coordinador presenta el nuevo")
  void alCambiarDeServicioElPanelPresentaElNuevo() {
    var servicioInicial = servicioConCupo(2);
    var servicioNuevo = servicioConCupo(2);
    var coordinador = coordinadorDe(servicioInicial.id());

    actuarComo(coordinador.correo());
    assertThat(coordinadores.listarResidentes().servicio()).isEqualTo(servicioInicial.nombre());

    actuarComo("admin@husi.com");
    usuarios.actualizar(
      coordinador.id(),
      datosDeCoordinador(coordinador.correo(), servicioNuevo.id())
    );

    actuarComo(coordinador.correo());
    assertThat(coordinadores.listarResidentes().servicio()).isEqualTo(servicioNuevo.nombre());
  }

  @Test
  @DisplayName("CU03-EX3 Un estado inválido responde 400")
  void unEstadoInvalidoResponde400() {
    var coordinador = coordinadorDe(servicioConCupo(2).id());
    debeFallar(HttpStatus.BAD_REQUEST, "Estado inválido", () ->
      usuarios.cambiarEstado(coordinador.id(), "Suspendido")
    );
  }

  @Test
  @DisplayName("CU03-EX3 Un usuario inexistente responde 404")
  void unUsuarioInexistenteResponde404() {
    debeFallar(HttpStatus.NOT_FOUND, "No se encontró el usuario", () ->
      usuarios.consultar(ID_INEXISTENTE)
    );
    debeFallar(HttpStatus.NOT_FOUND, "No se encontró el usuario", () ->
      usuarios.eliminarDefinitivamente(ID_INEXISTENTE)
    );
  }

  @Test
  @DisplayName("CU03-EX6 La respuesta nunca incluye la contraseña ni su hash")
  void laRespuestaNoIncluyeLaContrasena() {
    var creado = coordinadorDe(servicioConCupo(2).id());

    String creadoEnJson = jsonMapper.writeValueAsString(creado);
    String consultadoEnJson = jsonMapper.writeValueAsString(usuarios.consultar(creado.id()));

    for (String json : new String[] { creadoEnJson, consultadoEnJson }) assertThat(json)
      .doesNotContainIgnoringCase("contrasena")
      .doesNotContain(CONTRASENA_VALIDA)
      .doesNotContain("$2a$");
  }
}
