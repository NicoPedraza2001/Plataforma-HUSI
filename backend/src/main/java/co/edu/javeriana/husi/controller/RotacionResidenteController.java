package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.model.entity.RotacionId;
import co.edu.javeriana.husi.service.RotacionResidenteService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rotaciones/gestion/{asignatura}/{inicio}/{fin}")
public class RotacionResidenteController {

  private final RotacionResidenteService rotacionResidenteService;

  @GetMapping("/residentes")
  public PaginaResponse<ResidenteRotacionResponse> listarResidentes(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin,
    @RequestParam(defaultValue = "1") int pagina,
    @RequestParam(defaultValue = "false") boolean espera
  ) {
    return rotacionResidenteService.listar(
      new RotacionId(asignatura, inicio, fin),
      pagina,
      espera
    );
  }

  @GetMapping("/candidatos")
  public List<CatalogoResponse> candidatos(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin,
    @RequestParam(name = "q", defaultValue = "") String busqueda
  ) {
    return rotacionResidenteService.candidatos(new RotacionId(asignatura, inicio, fin), busqueda);
  }

  @PostMapping("/residentes")
  @ResponseStatus(HttpStatus.CREATED)
  public void asignar(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin,
    @Valid @RequestBody AsignarResidenteRequest request
  ) {
    rotacionResidenteService.asignar(new RotacionId(asignatura, inicio, fin), request.residenteId());
  }

  @PatchMapping("/residentes/{residente}/estado")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cambiarEstadoResidente(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin,
    @PathVariable Integer residente,
    @Valid @RequestBody EstadoAsignacionRequest request
  ) {
    rotacionResidenteService.cambiarEstado(
      new RotacionId(asignatura, inicio, fin),
      residente,
      request.estado()
    );
  }

  @DeleteMapping("/residentes/{residente}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarResidente(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin,
    @PathVariable Integer residente
  ) {
    rotacionResidenteService.eliminar(new RotacionId(asignatura, inicio, fin), residente);
  }

  @GetMapping("/espera")
  public List<EsperaServicioResponse> listarEsperaDelServicio(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin
  ) {
    return rotacionResidenteService.listarEsperaDelServicio(new RotacionId(asignatura, inicio, fin));
  }

  @PostMapping("/espera")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void trasladarEspera(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin,
    @Valid @RequestBody TrasladoEsperaRequest request
  ) {
    rotacionResidenteService.trasladarEspera(new RotacionId(asignatura, inicio, fin), request);
  }
}
