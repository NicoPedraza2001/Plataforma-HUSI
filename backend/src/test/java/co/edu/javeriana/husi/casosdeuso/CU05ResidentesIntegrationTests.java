package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.dto.request.ResidenteRequest;
import jakarta.validation.Validator;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/** CU-05 Gestionar residentes (RF-13 a RF-16). */
class CU05ResidentesIntegrationTests extends BaseCasoDeUso {

  private static final String CEDULA = "CC";

  @Autowired
  Validator validador;

  private ResidenteRequest datosDeResidente(
    String tipoDocumento,
    String numeroDocumento,
    String perfilAcademico
  ) {
    return new ResidenteRequest(
      "Residente de prueba",
      tipoDocumento,
      numeroDocumento,
      perfilAcademico,
      programaNuevo().id()
    );
  }

  private String documentoNuevo() {
    return UUID.randomUUID().toString();
  }

  @Test
  @DisplayName("CU05-FA1 El residente se guarda con perfil académico o sin él")
  void seGuardaConPerfilOSinEl() {
    var sinPerfil = residentes.crear(datosDeResidente(CEDULA, documentoNuevo(), null));
    var conPerfil = residentes.crear(datosDeResidente(CEDULA, documentoNuevo(), "Posgrado"));

    assertThat(sinPerfil.perfilAcademico()).isNull();
    assertThat(conPerfil.perfilAcademico()).isEqualTo("Posgrado");
  }

  @Test
  @DisplayName("CU05-FA2 Un pasaporte con letras se acepta")
  void unPasaporteConLetrasSeAcepta() {
    String tipoPasaporte = jdbc.queryForObject(
      "select codigo from tipo_documento where lower(nombre) like '%pasaporte%'",
      String.class
    );
    String pasaporte = "AB" + UUID.randomUUID().toString().substring(0, 8);

    var creado = residentes.crear(datosDeResidente(tipoPasaporte, pasaporte, null));

    assertThat(creado.numeroDocumento()).isEqualToIgnoringCase(pasaporte);
  }

  @Test
  @DisplayName("CU05-FA4 Un residente desactivado conserva sus rotaciones")
  void unResidenteDesactivadoConservaSusRotaciones() {
    var rotacion = rotacionConCupo(2);
    int residente = asignarResidentesNuevos(rotacion, 1).getFirst();

    residentes.cambiarEstado(residente, "Inactivo");

    assertThat(residentes.consultar(residente).estado()).isEqualTo("Inactivo");
    assertThat(estadoDe(rotacion, residente)).isEqualTo("Asignado");
  }

  @Test
  @DisplayName("CU05-EX3 Un tipo de documento o un perfil académico inexistentes responden 400")
  void unCatalogoInexistenteResponde400() {
    debeFallar(HttpStatus.BAD_REQUEST, "tipo de documento no existe", () ->
      residentes.crear(datosDeResidente("ZZ", documentoNuevo(), null))
    );
    debeFallar(HttpStatus.BAD_REQUEST, "perfil académico no existe", () ->
      residentes.crear(datosDeResidente(CEDULA, documentoNuevo(), "Perfil inexistente"))
    );
  }

  @Test
  @DisplayName("CU05-EX4 Campos vacíos, solo con espacios o demasiado largos no pasan la validación")
  void losCamposInvalidosNoPasanLaValidacion() {
    int programa = programaNuevo().id();
    String[][] casos = {
      { "   ", CEDULA, documentoNuevo(), "nombre" },
      { "x".repeat(201), CEDULA, documentoNuevo(), "nombre" },
      { "Residente", "   ", documentoNuevo(), "tipoDocumento" },
      { "Residente", CEDULA, "   ", "numeroDocumento" },
      { "Residente", CEDULA, "1".repeat(51), "numeroDocumento" },
    };
    for (String[] caso : casos) assertThat(
      validador.validate(new ResidenteRequest(caso[0], caso[1], caso[2], null, programa))
    )
      .as(caso[3])
      .extracting(violacion -> violacion.getPropertyPath().toString())
      .contains(caso[3]);
  }

  @Test
  @DisplayName("CU05-EX6 Un estado inválido responde 400 y un residente inexistente 404")
  void unEstadoInvalidoOUnResidenteInexistenteDanError() {
    int residente = residenteNuevo();
    debeFallar(HttpStatus.BAD_REQUEST, "Estado inválido", () ->
      residentes.cambiarEstado(residente, "Suspendido")
    );
    debeFallar(HttpStatus.NOT_FOUND, "No se encontró el residente", () ->
      residentes.consultar(ID_INEXISTENTE)
    );
  }

  @Test
  @DisplayName("CU05-EX7 Un residente inactivo no aparece entre los candidatos de una rotación")
  void unResidenteInactivoNoEsCandidato() {
    var rotacion = rotacionConCupo(2);
    int activo = residenteNuevo();
    int inactivo = residenteNuevo();
    residentes.cambiarEstado(inactivo, "Inactivo");

    // El listado trae 30 candidatos como máximo: cada uno se busca por su documento.
    assertThat(asignaciones.candidatos(rotacion, residentes.consultar(activo).numeroDocumento()))
      .extracting("id")
      .containsExactly(activo);
    assertThat(
      asignaciones.candidatos(rotacion, residentes.consultar(inactivo).numeroDocumento())
    ).isEmpty();
  }
}
