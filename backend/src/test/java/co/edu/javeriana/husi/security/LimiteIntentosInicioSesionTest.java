package co.edu.javeriana.husi.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Prueba unitaria del bloqueo por intentos fallidos (RF-02). */
class LimiteIntentosInicioSesionTest {

  private static final String CORREO = "persona@example.test";

  private final LimiteIntentosInicioSesion limite = new LimiteIntentosInicioSesion(5, 15);

  private void fallar(LimiteIntentosInicioSesion limiteDeIntentos, int veces) {
    for (int i = 0; i < veces; i++) limiteDeIntentos.registrarFallo(CORREO);
  }

  @Test
  @DisplayName("RF-02 El cuarto fallo todavía no bloquea")
  void elCuartoFalloNoBloquea() {
    fallar(limite, 4);
    assertThat(limite.estaBloqueado(CORREO)).isFalse();
  }

  @Test
  @DisplayName("RF-02 El quinto fallo activa el bloqueo")
  void elQuintoFalloBloquea() {
    fallar(limite, 5);
    assertThat(limite.estaBloqueado(CORREO)).isTrue();
    assertThat(limite.minutosBloqueo()).isEqualTo(15);
  }

  @Test
  @DisplayName("CU01-FA1 Un ingreso correcto reinicia el contador")
  void unIngresoCorrectoReiniciaElContador() {
    fallar(limite, 4);
    limite.registrarExito(CORREO);
    fallar(limite, 4);
    assertThat(limite.estaBloqueado(CORREO)).isFalse();
  }

  @Test
  @DisplayName("CU01-FA2 El correo se compara sin distinguir mayúsculas ni espacios")
  void elCorreoSeNormaliza() {
    fallar(limite, 5);
    assertThat(limite.estaBloqueado("  PERSONA@example.TEST ")).isTrue();
  }

  @Test
  @DisplayName("CU01-EX4 El bloqueo de un correo no afecta a otro")
  void elBloqueoNoAfectaAOtroCorreo() {
    fallar(limite, 5);
    assertThat(limite.estaBloqueado("otra@example.test")).isFalse();
  }

  @Test
  @DisplayName("CU01-FA3 Un bloqueo vencido deja de bloquear y devuelve todos los intentos")
  void unBloqueoVencidoDevuelveLosIntentos() {
    // Con 0 minutos de bloqueo, el bloqueo vence en el mismo instante en que se activa.
    var limiteQueVenceDeInmediato = new LimiteIntentosInicioSesion(2, 0);
    fallar(limiteQueVenceDeInmediato, 2);
    assertThat(limiteQueVenceDeInmediato.estaBloqueado(CORREO)).isFalse();

    fallar(limiteQueVenceDeInmediato, 1);

    assertThat(
      ReflectionTestUtils.getField(limiteQueVenceDeInmediato, "intentosPorCorreo").toString()
    ).contains("fallidos=1");
  }
}
