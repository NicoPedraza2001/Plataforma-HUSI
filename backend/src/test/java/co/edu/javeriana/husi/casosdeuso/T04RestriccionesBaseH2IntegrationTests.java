package co.edu.javeriana.husi.casosdeuso;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Comprueba las restricciones de la base sobre H2, con los triggers Java del perfil local. */
@SpringBootTest(properties = BaseCasoDeUso.BASE_H2)
@ActiveProfiles("local")
class T04RestriccionesBaseH2IntegrationTests extends T04RestriccionesBaseIntegrationTests {}
