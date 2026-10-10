package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.response.HistorialDetalleResponse;
import co.edu.javeriana.husi.service.HistorialAuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

  /** El asistente de Excel se cerró sin confirmar (también por navigator.sendBeacon). */
  @PostMapping("/importaciones/{lote}/cancelar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelarImportacion(@PathVariable String lote) {
    historial.cancelarImportacion(lote);
  }
}
