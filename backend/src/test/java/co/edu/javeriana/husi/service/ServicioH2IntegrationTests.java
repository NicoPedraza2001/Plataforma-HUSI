package co.edu.javeriana.husi.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Las mismas reglas de cupo simultáneo, contra los triggers Java del perfil local. */
@SpringBootTest(
  properties = "spring.datasource.url=jdbc:h2:mem:servicio;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;OPTIMIZE_REUSE_RESULTS=0;DB_CLOSE_DELAY=-1"
)
@ActiveProfiles("local")
class ServicioH2IntegrationTests extends ServicioIntegrationTests {}
