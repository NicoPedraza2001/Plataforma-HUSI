package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.response.HistorialDetalleResponse;
import co.edu.javeriana.husi.service.HistorialAuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Detalle de un movimiento. El listado paginado (GET /historial) vive en ConsultaController. */
@RestController
@RequestMapping("/historial")
@RequiredArgsConstructor
public class HistorialAuditoriaController {

  private final HistorialAuditoriaService historial;

  @GetMapping("/{id}")
  public HistorialDetalleResponse consultar(@PathVariable Integer id) {
    return historial.consultar(id);
  }
}
