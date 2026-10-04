package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.service.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/servicios")
@RequiredArgsConstructor
public class ServicioController {

  private final ServicioService servicioService;

  @GetMapping("/catalogo")
  public java.util.List<CatalogoResponse> catalogo(
    @RequestParam(defaultValue = "true") boolean soloActivos
  ) {
    return servicioService.catalogo(soloActivos);
  }

  @GetMapping("/{id}")
  public ServicioResponse consultar(@PathVariable Integer id) {
    return servicioService.consultar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ServicioResponse crear(@Valid @RequestBody ServicioRequest request) {
    return servicioService.crear(request);
  }

  @PutMapping("/{id}")
  public ServicioResponse actualizar(
    @PathVariable Integer id,
    @Valid @RequestBody ServicioRequest request
  ) {
    return servicioService.actualizar(id, request);
  }

  @PatchMapping("/{id}/estado")
  public ServicioResponse cambiarEstado(
    @PathVariable Integer id,
    @Valid @RequestBody EstadoRequest request
  ) {
    return servicioService.cambiarEstado(id, request.estado());
  }

  @DeleteMapping("/{id}/definitivo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Integer id) {
    servicioService.eliminarDefinitivamente(id);
  }
}
