package co.edu.javeriana.husi.controller;
import co.edu.javeriana.husi.exception.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
public class SesionController {
 @GetMapping("/sesion/perfil") public void perfil() {
  // El proyecto aún no implementa autenticación. No asumir la identidad del usuario 1.
  throw new NegocioException(HttpStatus.UNAUTHORIZED,"La gestión del perfil estará disponible cuando se integre el inicio de sesión.");
 }
}
