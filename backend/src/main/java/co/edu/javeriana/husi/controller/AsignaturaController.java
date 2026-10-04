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

  private final AsignaturaService asignaturaService;

  @GetMapping("/{id}")
  public AsignaturaResponse consultar(@PathVariable Integer id) {
    return asignaturaService.consultar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AsignaturaResponse crear(
    @Valid @RequestBody AsignaturaRequest request
  ) {
    return asignaturaService.crear(request);
  }

  @PutMapping("/{id}")
  public AsignaturaResponse actualizar(
    @PathVariable Integer id,
    @Valid @RequestBody AsignaturaRequest request
  ) {
    return asignaturaService.actualizar(id, request);
  }

  @PatchMapping("/{id}/estado")
  public AsignaturaResponse cambiarEstado(
    @PathVariable Integer id,
    @Valid @RequestBody EstadoRequest request
  ) {
    return asignaturaService.cambiarEstado(id, request.estado());
  }

  @DeleteMapping("/{id}/definitivo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Integer id) {
    asignaturaService.eliminarDefinitivamente(id);
  }
}
