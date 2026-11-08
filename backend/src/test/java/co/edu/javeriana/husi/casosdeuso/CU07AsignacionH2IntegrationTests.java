package co.edu.javeriana.husi.casosdeuso;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Comprueba las reglas de asignación sobre H2, con los triggers Java del perfil local. */
@SpringBootTest(properties = BaseCasoDeUso.BASE_H2)
@ActiveProfiles("local")
class CU07AsignacionH2IntegrationTests extends CU07AsignacionIntegrationTests {}
