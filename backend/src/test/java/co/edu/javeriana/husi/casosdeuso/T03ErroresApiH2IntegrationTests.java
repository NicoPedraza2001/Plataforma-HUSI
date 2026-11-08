package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-03 Respuestas de error de la API (RNF-14): claras y sin detalles internos. */
class T03ErroresApiH2IntegrationTests extends BaseCasoDeUsoPorApi {

  private static final String RESIDENTE_INEXISTENTE = "residentes/999999";

  private void sinDetallesInternos(String cuerpo) {
    assertThat(cuerpo)
      .doesNotContainIgnoringCase("exception")
      .doesNotContainIgnoringCase("select ")
      .doesNotContain("co.edu.javeriana")
      .doesNotContain("\tat ");
  }

  @Test
  @DisplayName("T-03 Un JSON mal formado responde 400 sin detalles internos")
  void unJsonMalFormadoResponde400() throws Exception {
    var respuesta = clienteAdministrador().enviar("POST", "servicios", "{esto no es json");
    assertThat(respuesta.statusCode()).isEqualTo(400);
    assertThat(respuesta.body()).contains("El formato de los datos no es válido");
    sinDetallesInternos(respuesta.body());
  }

  @Test
  @DisplayName("T-03 Un tipo incorrecto en la ruta responde 400 sin detalles internos")
  void unTipoIncorrectoEnLaRutaResponde400() throws Exception {
    var respuesta = clienteAdministrador().get("residentes/no-es-un-numero");
    assertThat(respuesta.statusCode()).isEqualTo(400);
    sinDetallesInternos(respuesta.body());
  }

  @Test
  @DisplayName("T-03 Un formulario incompleto señala cada campo y su razón (RNF-14)")
  void unFormularioIncompletoSenalaLosCampos() throws Exception {
    var respuesta = clienteAdministrador().enviar(
      "POST",
      "universidades",
      "{\"nombre\":\"   \",\"pais\":\"\"}"
    );
    assertThat(respuesta.statusCode()).isEqualTo(400);
    assertThat(respuesta.body())
      .contains("Revisa los datos del formulario")
      .contains("\"nombre\":\"Escribe el nombre de la universidad.\"")
      .contains("\"pais\":\"Escribe el país.\"");
  }

  @Test
  @DisplayName("T-03 Un registro inexistente responde 404 con el mensaje del negocio")
  void unRegistroInexistenteResponde404() throws Exception {
    var respuesta = clienteAdministrador().get(RESIDENTE_INEXISTENTE);
    assertThat(respuesta.statusCode()).isEqualTo(404);
    assertThat(respuesta.body()).contains("No se encontró el residente");
    sinDetallesInternos(respuesta.body());
  }
}
