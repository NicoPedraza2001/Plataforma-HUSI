package co.edu.javeriana.husi.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Frena los ataques de fuerza bruta: tras {@code husi.seguridad.inicio-sesion.intentos-maximos}
 * contraseñas incorrectas seguidas para un mismo correo, ese correo queda bloqueado durante
 * {@code husi.seguridad.inicio-sesion.minutos-bloqueo}, aunque después se escriba la contraseña
 * correcta. Un inicio de sesión exitoso reinicia el contador.
 *
 * <p>Los contadores viven en memoria: se pierden al reiniciar el backend y no se comparten si se
 * despliegan varias instancias.
 */
@Component
public class LimiteIntentosInicioSesion {

  private record Intentos(int fallidos, Instant bloqueadoHasta) {}

  private final Map<String, Intentos> intentosPorCorreo = new ConcurrentHashMap<>();
  private final int intentosMaximos;
  private final Duration duracionBloqueo;

  public LimiteIntentosInicioSesion(
    @Value("${husi.seguridad.inicio-sesion.intentos-maximos}") int intentosMaximos,
    @Value("${husi.seguridad.inicio-sesion.minutos-bloqueo}") long minutosBloqueo
  ) {
    this.intentosMaximos = intentosMaximos;
    this.duracionBloqueo = Duration.ofMinutes(minutosBloqueo);
  }

  public boolean estaBloqueado(String correo) {
    Intentos intentos = intentosPorCorreo.get(clave(correo));
    return (
      intentos != null &&
      intentos.bloqueadoHasta() != null &&
      Instant.now().isBefore(intentos.bloqueadoHasta())
    );
  }

  public void registrarFallo(String correo) {
    intentosPorCorreo.compute(clave(correo), (clave, anteriores) -> {
      // Un bloqueo vencido vuelve a dar todos los intentos.
      int fallidos = anteriores == null || anteriores.bloqueadoHasta() != null
        ? 1
        : anteriores.fallidos() + 1;
      Instant bloqueadoHasta = fallidos >= intentosMaximos
        ? Instant.now().plus(duracionBloqueo)
        : null;
      return new Intentos(fallidos, bloqueadoHasta);
    });
  }

  public void registrarExito(String correo) {
    intentosPorCorreo.remove(clave(correo));
  }

  public long minutosBloqueo() {
    return duracionBloqueo.toMinutes();
  }

  private String clave(String correo) {
    return correo.strip().toLowerCase(Locale.ROOT);
  }
}
