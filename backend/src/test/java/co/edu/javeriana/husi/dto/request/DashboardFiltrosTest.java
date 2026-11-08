package co.edu.javeriana.husi.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Prueba unitaria de los filtros del tablero (RF-30). */
class DashboardFiltrosTest {

  @Test
  @DisplayName("RF-30 Sin parámetros no se aplica ningún filtro")
  void sinParametrosNoHayFiltros() {
    assertThat(DashboardFiltros.de(Map.of())).isEqualTo(
      new DashboardFiltros(null, null, null, null, null, false)
    );
  }

  @Test
  @DisplayName("RF-30 Lee todos los filtros y quita los espacios")
  void leeTodosLosFiltros() {
    var filtros = DashboardFiltros.de(
      Map.of(
        "periodo",
        " 2026-1 ",
        "universidad",
        "Javeriana",
        "programa",
        "Pediatría",
        "servicio",
        "Cirugía",
        "asignaturaId",
        " 12 "
      )
    );
    assertThat(filtros).isEqualTo(
      new DashboardFiltros("2026-1", "Javeriana", "Pediatría", "Cirugía", 12, false)
    );
  }

  @Test
  @DisplayName("RF-34 El alcance del coordinador se limita a su servicio")
  void elAlcanceDelCoordinadorSeLimitaASuServicio() {
    assertThat(DashboardFiltros.soloDelServicio("Cirugía")).isEqualTo(
      new DashboardFiltros(null, null, null, "Cirugía", null, true)
    );
  }

  @Test
  @DisplayName("RF-30 Un filtro vacío cuenta como sin filtro")
  void unFiltroVacioNoSeAplica() {
    var filtros = DashboardFiltros.de(Map.of("servicio", "   ", "asignaturaId", ""));
    assertThat(filtros.servicio()).isNull();
    assertThat(filtros.asignaturaId()).isNull();
  }
}
