package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.service.ResidenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/residentes")
@RequiredArgsConstructor
public class ResidenteController {

  private final ResidenteService service;

  @GetMapping("/{id}")
  public ResidenteResponse consultar(@PathVariable Integer id) {
    return service.consultar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResidenteResponse crear(@Valid @RequestBody ResidenteRequest request) {
    return service.crear(request);
  }

  @PutMapping("/{id}")
  public ResidenteResponse actualizar(
    @PathVariable Integer id,
    @Valid @RequestBody ResidenteRequest request
  ) {
    return service.actualizar(id, request);
  }

  @PatchMapping("/{id}/estado")
  public ResidenteResponse estado(
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
