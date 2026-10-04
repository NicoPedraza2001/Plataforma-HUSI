package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.request.CargaInicialRequest;
import co.edu.javeriana.husi.dto.response.CargaInicialResponse;
import co.edu.javeriana.husi.service.CargaInicialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rotaciones/carga-inicial")
@RequiredArgsConstructor
public class CargaInicialController {

  private final CargaInicialService cargaInicialService;

  @PostMapping("/validar")
  public CargaInicialResponse validar(
    @Valid @RequestBody CargaInicialRequest request
  ) {
    return cargaInicialService.validar(request);
  }

  @PostMapping("/confirmar")
  public CargaInicialResponse confirmar(
    @Valid @RequestBody CargaInicialRequest request
  ) {
    return cargaInicialService.confirmar(request);
  }
}
