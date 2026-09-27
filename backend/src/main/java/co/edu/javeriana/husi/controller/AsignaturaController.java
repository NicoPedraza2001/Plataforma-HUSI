package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.service.AsignaturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/asignaturas")
@RequiredArgsConstructor
public class AsignaturaController {

  private final AsignaturaService service;

  @GetMapping("/{id}")
  public AsignaturaResponse consultar(@PathVariable Integer id) {
    return service.consultar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AsignaturaResponse crear(
    @Valid @RequestBody AsignaturaRequest request
  ) {
    return service.crear(request);
  }

  @PutMapping("/{id}")
  public AsignaturaResponse actualizar(
    @PathVariable Integer id,
    @Valid @RequestBody AsignaturaRequest request
  ) {
    return service.actualizar(id, request);
  }

  @PatchMapping("/{id}/estado")
  public AsignaturaResponse estado(
    @PathVariable Integer id,
    @Valid @RequestBody EstadoRequest request
  ) {
    return service.cambiarEstado(id, request.estado());
  }

  @DeleteMapping("/{id}/definitivo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Integer id) {
    service.eliminarDefinitivamente(id);
  }
}
