package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.service.DashboardService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboard;

  @GetMapping("/dashboard")
  public Map<String, Object> consultar(
    @RequestParam Map<String, String> filtros
  ) {
    if (
      !Set.of("periodo", "universidad", "servicio").containsAll(
        filtros.keySet()
      )
    ) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "Filtro de dashboard no disponible."
    );
    return dashboard.consultar(filtros);
  }
}
