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

  private final CoordinadorService coordinadorService;

  @GetMapping("/usuarios-disponibles")
  public java.util.List<CatalogoResponse> usuariosDisponibles() {
    return coordinadorService.usuariosDisponibles();
  }

  @GetMapping("/{id}")
  public CoordinadorResponse consultar(@PathVariable Integer id) {
    return coordinadorService.consultar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CoordinadorResponse crear(
    @Valid @RequestBody CoordinadorRequest request
  ) {
    return coordinadorService.crear(request);
  }

  @PutMapping("/{id}")
  public CoordinadorResponse actualizar(
    @PathVariable Integer id,
    @Valid @RequestBody CoordinadorRequest request
  ) {
    return coordinadorService.actualizar(id, request);
  }

  @PatchMapping("/{id}/estado")
  public CoordinadorResponse cambiarEstado(
    @PathVariable Integer id,
    @Valid @RequestBody EstadoRequest request
  ) {
    return coordinadorService.cambiarEstado(id, request.estado());
  }

  @DeleteMapping("/{id}/definitivo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Integer id) {
    coordinadorService.eliminarDefinitivamente(id);
  }
}
