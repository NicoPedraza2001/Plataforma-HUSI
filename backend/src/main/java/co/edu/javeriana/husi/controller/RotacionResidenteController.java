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

  private final RotacionResidenteService service;

  @GetMapping("/residentes")
  public PaginaResponse<ResidenteRotacionResponse> listar(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin,
    @RequestParam(defaultValue = "1") int pagina,
    @RequestParam(defaultValue = "false") boolean espera
  ) {
    return service.listar(
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
    @RequestParam(defaultValue = "") String q
  ) {
    return service.candidatos(new RotacionId(asignatura, inicio, fin), q);
  }

  @PostMapping("/residentes")
  @ResponseStatus(HttpStatus.CREATED)
  public void asignar(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin,
    @Valid @RequestBody AsignarResidenteRequest d
  ) {
    service.asignar(new RotacionId(asignatura, inicio, fin), d.residenteId());
  }

  @PatchMapping("/residentes/{residente}/estado")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void estado(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin,
    @PathVariable Integer residente,
    @Valid @RequestBody EstadoAsignacionRequest d
  ) {
    service.estado(
      new RotacionId(asignatura, inicio, fin),
      residente,
      d.estado()
    );
  }

  @DeleteMapping("/residentes/{residente}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin,
    @PathVariable Integer residente
  ) {
    service.eliminar(new RotacionId(asignatura, inicio, fin), residente);
  }

  @GetMapping("/espera")
  public List<EsperaServicioResponse> espera(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin
  ) {
    return service.espera(new RotacionId(asignatura, inicio, fin));
  }

  @PostMapping("/espera")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void trasladarEspera(
    @PathVariable Integer asignatura,
    @PathVariable LocalDate inicio,
    @PathVariable LocalDate fin,
    @Valid @RequestBody TrasladoEsperaRequest d
  ) {
    service.trasladarEspera(new RotacionId(asignatura, inicio, fin), d);
  }
}
