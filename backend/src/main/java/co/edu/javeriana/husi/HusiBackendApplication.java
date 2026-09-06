package co.edu.javeriana.husi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API REST del Sistema de Gestion de Residentes del HUSI.
 *
 * Al estar en el paquete raiz, Spring detecta automaticamente los componentes
 * (@RestController, @Service, @Repository, @Configuration) de todos los
 * subpaquetes, por lo que no hace falta configurar escaneo manual.
 */
@SpringBootApplication
public class HusiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HusiBackendApplication.class, args);
    }
}
