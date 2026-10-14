package co.edu.javeriana.husi.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Orígenes que pueden llamar a la API desde otro dominio. Spring Security la aplica antes de
 * revisar la sesión. {@code allowCredentials} deja viajar la cookie de sesión; por eso los
 * orígenes deben ser explícitos, nunca {@code *}.
 */
@Configuration
public class CorsConfig {

  @Value("${husi.cors.allowed-origins}")
  private String[] origins;

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    var configuracion = new CorsConfiguration();
    configuracion.setAllowedOrigins(List.of(origins));
    configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuracion.setAllowedHeaders(
      List.of(
        "Content-Type",
        "X-XSRF-TOKEN",
        LoteImportacionFiltro.CABECERA_LOTE,
        LoteImportacionFiltro.CABECERA_ARCHIVO
      )
    );
    configuracion.setAllowCredentials(true);
    var origen = new UrlBasedCorsConfigurationSource();
    origen.registerCorsConfiguration("/**", configuracion);
    return origen;
  }
}
