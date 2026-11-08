package co.edu.javeriana.husi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.repository.DashboardRepository.ParCruzado;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Prueba unitaria de la descripción de los cruces de fechas (RF-31). */
class CrucesDashboardTest {

  private ParCruzado par(int residenteId, LocalDate desde, LocalDate hasta) {
    return new ParCruzado(
      residenteId,
      "Residente " + residenteId,
      "Programa",
      "Cadera",
      "Alto Riesgo",
      "Columna",
      "Ortopedia",
      desde,
      hasta
    );
  }

  @Test
  @DisplayName("RF-31 Describe el cruce con sus días, contando el primero y el último")
  void describeElCruceConSusDias() {
    assertThat(
      CrucesDashboard.describir(par(1, LocalDate.of(2025, 8, 1), LocalDate.of(2025, 9, 30)))
    ).isEqualTo("Cadera (Alto Riesgo) y Columna (Ortopedia): del 2025-08-01 al 2025-09-30, 61 días");
  }

  @Test
  @DisplayName("RF-31 Un cruce de un solo día se escribe en singular")
  void unCruceDeUnDiaVaEnSingular() {
    LocalDate dia = LocalDate.of(2025, 8, 1);
    assertThat(CrucesDashboard.describir(par(1, dia, dia))).endsWith(", 1 día");
  }

  @Test
  @DisplayName("RF-31 Junta los cruces de un residente en un registro, uno por línea")
  void juntaLosCrucesDeUnResidente() {
    LocalDate dia = LocalDate.of(2025, 8, 1);
    var revisar = CrucesDashboard.porResidente(
      List.of(par(1, dia, dia), par(2, dia, dia), par(1, dia.plusDays(1), dia.plusDays(1)))
    );

    assertThat(revisar).hasSize(2);
    assertThat(revisar.getFirst().residenteId()).isEqualTo(1);
    assertThat(revisar.getFirst().motivo()).isEqualTo("cruce");
    assertThat(revisar.getFirst().detalle().split("\n")).hasSize(2);
    assertThat(revisar.get(1).detalle().split("\n")).hasSize(1);
  }

  @Test
  @DisplayName("RF-31 Sin cruces no hay registros")
  void sinCrucesNoHayRegistros() {
    assertThat(CrucesDashboard.porResidente(List.of())).isEmpty();
  }
}
