package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.response.DashboardResponse;
import co.edu.javeriana.husi.dto.response.RelacionFiltrosResponse;
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
  public DashboardResponse consultar(
    @RequestParam Map<String, String> filtros
  ) {
    if (
      !Set.of(
        "periodo",
        "universidad",
        "programa",
        "servicio",
        "asignaturaId"
      ).containsAll(filtros.keySet())
    ) throw new NegocioException(
      HttpStatus.BAD_REQUEST,
      "Filtro de dashboard no disponible."
    );
    return dashboard.consultar(filtros);
  }

  /** Programa · servicio · asignatura que aparecen juntos en rotaciones, para encadenar filtros. */
  @GetMapping("/dashboard/relaciones")
  public List<RelacionFiltrosResponse> relaciones() {
    return dashboard.relaciones();
  }
}
