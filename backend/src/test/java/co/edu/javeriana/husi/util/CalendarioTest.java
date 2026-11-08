package co.edu.javeriana.husi.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Prueba unitaria de la fecha del hospital (RF-21, RF-23). */
class CalendarioTest {

  @Test
  @DisplayName("RF-21 Hoy es la fecha de Bogotá")
  void hoyEsLaFechaDeBogota() {
    assertThat(Calendario.hoy()).isEqualTo(LocalDate.now(ZoneId.of("America/Bogota")));
  }

  @Test
  @DisplayName("RF-23 Una rotación que termina hoy todavía no ha finalizado")
  void unaRotacionQueTerminaHoyNoHaFinalizado() {
    assertThat(Calendario.finalizada(Calendario.hoy())).isFalse();
    assertThat(Calendario.finalizada(Calendario.hoy().plusDays(1))).isFalse();
  }

  @Test
  @DisplayName("RF-23 Una rotación que terminó ayer ya finalizó")
  void unaRotacionQueTerminoAyerYaFinalizo() {
    assertThat(Calendario.finalizada(Calendario.hoy().minusDays(1))).isTrue();
  }

  @Test
  @DisplayName("RF-21 El formato de los mensajes es el colombiano")
  void elFormatoEsElColombiano() {
    assertThat(Calendario.formato(LocalDate.of(2026, 3, 9))).isEqualTo("09/03/2026");
  }
}
