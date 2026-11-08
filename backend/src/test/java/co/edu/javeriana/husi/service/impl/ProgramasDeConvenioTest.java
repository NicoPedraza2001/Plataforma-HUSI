package co.edu.javeriana.husi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Prueba unitaria de los convenios según la universidad y su país (RF-08). */
class ProgramasDeConvenioTest {

  @ParameterizedTest
  @ValueSource(strings = { "Pontificia Universidad Javeriana", " pontificia universidad javeriana " })
  @DisplayName("RF-08 Reconoce a la universidad propia sin importar mayúsculas ni espacios")
  void reconoceLaUniversidadPropia(String nombre) {
    assertThat(ProgramasDeConvenio.esUniversidadPropia(nombre)).isTrue();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = { "", "Universidad Nacional" })
  @DisplayName("RF-08 Las demás universidades son externas")
  void lasDemasSonExternas(String nombre) {
    assertThat(ProgramasDeConvenio.esUniversidadPropia(nombre)).isFalse();
  }

  @ParameterizedTest
  @CsvSource({ "Colombia,Convenio Nacional", "' colombia ',Convenio Nacional", "España,Convenio Internacional" })
  @DisplayName("RF-08 El convenio corresponde al país de la universidad")
  void elConvenioCorrespondeAlPais(String pais, String convenio) {
    assertThat(ProgramasDeConvenio.convenioSegunPais(pais)).isEqualTo(convenio);
  }

  @Test
  @DisplayName("RF-08 Reconoce los convenios y rechaza cualquier otro nombre")
  void reconoceLosConvenios() {
    assertThat(ProgramasDeConvenio.buscarConvenio(" convenio nacional ")).contains("Convenio Nacional");
    assertThat(ProgramasDeConvenio.buscarConvenio("CONVENIO INTERNACIONAL")).contains(
      "Convenio Internacional"
    );
    assertThat(ProgramasDeConvenio.buscarConvenio("Especialización en Pediatría")).isEmpty();
    assertThat(ProgramasDeConvenio.buscarConvenio(null)).isEmpty();
  }

  @Test
  @DisplayName("RF-08 Un convenio acorde al país no da error")
  void unConvenioAcordeNoDaError() {
    assertThat(
      ProgramasDeConvenio.buscarErrorDeConvenioSegunPais("Convenio Nacional", "U. Nacional", "Colombia")
    ).isEmpty();
    assertThat(
      ProgramasDeConvenio.buscarErrorDeConvenioSegunPais("Convenio Internacional", "U. Madrid", "España")
    ).isEmpty();
  }

  @Test
  @DisplayName("RF-08 Un convenio internacional en universidad nacional da error")
  void convenioInternacionalEnUniversidadNacional() {
    assertThat(
      ProgramasDeConvenio.buscarErrorDeConvenioSegunPais("Convenio Internacional", "U. Nacional", "Colombia")
    ).get().asString().contains("es nacional").contains("Movilidad Nacional");
  }

  @Test
  @DisplayName("RF-08 Un convenio nacional en universidad internacional da error")
  void convenioNacionalEnUniversidadInternacional() {
    assertThat(
      ProgramasDeConvenio.buscarErrorDeConvenioSegunPais("Convenio Nacional", "U. Madrid", "España")
    ).get().asString().contains("es internacional").contains("Observador Internacional");
  }

  @Test
  @DisplayName("RF-08 Un programa que no es un convenio no se evalúa contra el país")
  void unProgramaQueNoEsConvenioNoSeEvalua() {
    assertThat(
      ProgramasDeConvenio.buscarErrorDeConvenioSegunPais("Pediatría", "U. Nacional", "Colombia")
    ).isEmpty();
  }
}
