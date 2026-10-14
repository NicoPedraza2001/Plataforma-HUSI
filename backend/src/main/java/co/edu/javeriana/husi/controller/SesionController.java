package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.request.ActualizacionCredencialesRequest;
import co.edu.javeriana.husi.dto.request.ActualizacionPerfilRequest;
import co.edu.javeriana.husi.dto.request.InicioSesionRequest;
import co.edu.javeriana.husi.dto.response.PerfilResponse;
import co.edu.javeriana.husi.service.SesionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Inicio y cierre de sesión, y ajustes de la cuenta propia. Las reglas de acceso están en security/ConfiguracionSeguridad. */
@RestController
@RequestMapping("/sesion")
@RequiredArgsConstructor
public class SesionController {

  private final SesionService sesionService;

  /** Única ruta de la API abierta sin sesión. */
  @PostMapping("/iniciar")
  public PerfilResponse iniciarSesion(
    @Valid @RequestBody InicioSesionRequest credenciales,
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    return sesionService.iniciarSesion(credenciales, request, response);
  }

  @PostMapping("/cerrar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cerrarSesion(HttpServletRequest request, HttpServletResponse response) {
    sesionService.cerrarSesion(request, response);
  }

  @GetMapping("/perfil")
  public PerfilResponse perfil() {
    return sesionService.perfil();
  }

  /** Pantalla de ajustes: nombres y/o apellidos del usuario en sesión. */
  @PatchMapping("/perfil")
  public PerfilResponse actualizarPerfil(@Valid @RequestBody ActualizacionPerfilRequest request) {
    return sesionService.actualizarPerfil(request);
  }

  /** Pantalla de ajustes: correo y/o contraseña; exige la contraseña actual. */
  @PatchMapping("/credenciales")
  public PerfilResponse actualizarCredenciales(
    @Valid @RequestBody ActualizacionCredencialesRequest request
  ) {
    return sesionService.actualizarCredenciales(request);
  }
}
