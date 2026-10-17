package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.response.ResidentesACargoResponse;
import co.edu.javeriana.husi.service.CoordinadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Panel del coordinador. Solo rol Coordinador (security/ConfiguracionSeguridad). */
@RestController
@RequestMapping("/coordinador")
@RequiredArgsConstructor
public class CoordinadorController {

  private final CoordinadorService coordinadorService;

  /** Rotaciones en curso hoy y próximas en el servicio del coordinador en sesión. */
  @GetMapping("/residentes")
  public ResidentesACargoResponse listarResidentes() {
    return coordinadorService.listarResidentes();
  }
}
