package co.edu.javeriana.husi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.repository.DashboardRepository.Rango;
import co.edu.javeriana.husi.repository.DashboardRepository.Tramo;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Prueba unitaria del mapa de ocupación por servicio (RF-31, RF-32). */
class OcupacionDashboardTest {

  private static final Rango ENERO = new Rango(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

  private Tramo tramo(String servicio, int cupo, int diaInicio, int diaFin) {
    return new Tramo(
      servicio,
      cupo,
      LocalDate.of(2025, 1, diaInicio),
      LocalDate.of(2025, 1, diaFin)
    );
  }

  @Test
  @DisplayName("RF-31 El primer semestre va de enero a junio")
  void elPrimerSemestreVaDeEneroAJunio() {
    assertThat(OcupacionDashboard.ventana("2025-1", Optional.empty())).contains(
      new Rango(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30))
    );
  }

  @Test
  @DisplayName("RF-31 El segundo semestre va de julio a diciembre")
  void elSegundoSemestreVaDeJulioADiciembre() {
    assertThat(OcupacionDashboard.ventana("2025-2", Optional.empty())).contains(
      new Rango(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 12, 31))
    );
  }

  @Test
  @DisplayName("RF-32 Sin periodo y sin asignaciones no hay mapa")
  void sinPeriodoNiAsignacionesNoHayMapa() {
    assertThat(OcupacionDashboard.ventana(null, Optional.empty())).isEmpty();
  }

  @Test
  @DisplayName("RF-32 Sin periodo cubre como máximo los últimos doce meses con asignaciones")
  void sinPeriodoCubreDoceMeses() {
    var asignaciones = new Rango(LocalDate.of(2020, 3, 10), LocalDate.of(2025, 6, 15));
    assertThat(OcupacionDashboard.ventana(null, Optional.of(asignaciones))).contains(
      new Rango(LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30))
    );
  }

  @Test
  @DisplayName("RF-32 Sin periodo y con pocas asignaciones arranca en el primer mes con datos")
  void conPocasAsignacionesArrancaEnElPrimerMes() {
    var asignaciones = new Rango(LocalDate.of(2025, 3, 10), LocalDate.of(2025, 4, 15));
    assertThat(OcupacionDashboard.ventana(null, Optional.of(asignaciones))).contains(
      new Rango(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 4, 30))
    );
  }

  @Test
  @DisplayName("RF-31 Cuenta los días sobre el cupo y el pico del mes")
  void cuentaLosDiasSobreElCupoYElPico() {
    var mapa = OcupacionDashboard.calcular(
      ENERO,
      List.of(tramo("Cirugía", 1, 1, 20), tramo("Cirugía", 1, 11, 31))
    );

    var fila = mapa.filas().getFirst();
    assertThat(mapa.meses()).containsExactly("2025-01");
    assertThat(fila.diasExceso()).isEqualTo(10);
    assertThat(fila.celdas().getFirst().pico()).isEqualTo(2);
    assertThat(fila.celdas().getFirst().porcentaje()).isEqualTo(200);
    assertThat(fila.maximo()).isEqualTo(200);
  }

  @Test
  @DisplayName("RF-31 Dos tramos consecutivos no se suman el mismo día")
  void dosTramosConsecutivosNoSeSuman() {
    var mapa = OcupacionDashboard.calcular(
      ENERO,
      List.of(tramo("Cirugía", 1, 1, 15), tramo("Cirugía", 1, 16, 31))
    );

    assertThat(mapa.filas().getFirst().diasExceso()).isZero();
    assertThat(mapa.filas().getFirst().maximo()).isEqualTo(100);
  }

  @Test
  @DisplayName("RF-32 Primero aparecen los servicios más presionados")
  void primeroLosServiciosMasPresionados() {
    var mapa = OcupacionDashboard.calcular(
      ENERO,
      List.of(tramo("Baja", 4, 1, 10), tramo("Alta", 1, 1, 10))
    );
    assertThat(mapa.filas()).extracting("servicio").containsExactly("Alta", "Baja");
  }

  @Test
  @DisplayName("RF-32 Un servicio con cupo cero no divide por cero")
  void unCupoCeroNoDividePorCero() {
    var mapa = OcupacionDashboard.calcular(ENERO, List.of(tramo("Sin cupo", 0, 1, 2)));
    assertThat(mapa.filas().getFirst().maximo()).isZero();
    assertThat(mapa.filas().getFirst().diasExceso()).isEqualTo(2);
  }

  @Test
  @DisplayName("RF-32 Un tramo puede terminar el último día de la ventana")
  void unTramoPuedeTerminarElUltimoDia() {
    var mapa = OcupacionDashboard.calcular(ENERO, List.of(tramo("Cirugía", 1, 31, 31)));
    assertThat(mapa.filas().getFirst().celdas().getFirst().pico()).isEqualTo(1);
  }
}
