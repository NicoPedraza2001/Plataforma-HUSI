package co.edu.javeriana.husi.casosdeuso;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Comprueba la reversión de la confirmación sobre H2. Usa su propia base porque el espía de la
 * clase padre levanta otro contexto de Spring, que volvería a crear las tablas en la compartida.
 */
@SpringBootTest(
  properties = "spring.datasource.url=jdbc:h2:mem:casosdeusoatomicidad;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;OPTIMIZE_REUSE_RESULTS=0;DB_CLOSE_DELAY=-1"
)
@ActiveProfiles("local")
class CU09AtomicidadH2IntegrationTests extends CU09AtomicidadIntegrationTests {}
