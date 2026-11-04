package co.edu.javeriana.husi;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
  properties = "spring.datasource.url=jdbc:h2:mem:carga;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;OPTIMIZE_REUSE_RESULTS=0;DB_CLOSE_DELAY=-1"
)
@ActiveProfiles("local")
class CargaInicialH2IntegrationTests extends CargaInicialIntegrationTests {}
