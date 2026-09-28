package co.edu.javeriana.husi.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/** Evita convertir silenciosamente números y booleanos en nombres o documentos. */
@Configuration
public class JsonEstrictoConfig {

  @Bean
  JsonMapperBuilderCustomizer tiposTextoEstrictos() {
    return builder ->
      builder.withCoercionConfig(LogicalType.Textual, config -> {
        config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
        config.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
        config.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
      });
  }
}
