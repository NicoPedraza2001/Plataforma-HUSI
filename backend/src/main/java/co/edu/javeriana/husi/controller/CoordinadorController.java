package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.service.CoordinadorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/coordinadores")
@RequiredArgsConstructor
public class CoordinadorController {

  private final CoordinadorService service;

  @GetMapping("/usuarios-disponibles")
  public java.util.List<CatalogoResponse> usuariosDisponibles() {
    return service.usuariosDisponibles();
  }

  @GetMapping("/{id}")
  public CoordinadorResponse consultar(@PathVariable Integer id) {
    return service.consultar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CoordinadorResponse crear(
    @Valid @RequestBody CoordinadorRequest request
  ) {
    return service.crear(request);
  }

  @PutMapping("/{id}")
  public CoordinadorResponse actualizar(
    @PathVariable Integer id,
    @Valid @RequestBody CoordinadorRequest request
  ) {
    return service.actualizar(id, request);
  }

  @PatchMapping("/{id}/estado")
  public CoordinadorResponse estado(
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
