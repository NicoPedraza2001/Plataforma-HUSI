package co.edu.javeriana.husi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que el contexto de Spring levanta sin errores.
 * Requiere la base de datos arriba (docker compose up -d postgres).
 */
@SpringBootTest
class HusiBackendApplicationTests {

  @Test
  void contextLoads() {}
}
