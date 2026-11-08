package co.edu.javeriana.husi.casosdeuso;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Comprueba las consultas del tablero sobre H2, con los triggers Java del perfil local. */
@SpringBootTest(properties = BaseCasoDeUso.BASE_H2)
@ActiveProfiles("local")
class CU10TableroH2IntegrationTests extends CU10TableroIntegrationTests {}
