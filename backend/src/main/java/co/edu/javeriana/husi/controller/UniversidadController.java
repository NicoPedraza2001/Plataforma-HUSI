package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.service.UniversidadService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/universidades")
@RequiredArgsConstructor
public class UniversidadController {

  private final UniversidadService universidades;

  // Mantiene el catálogo usado por formularios e importación existentes.
  @GetMapping
  public List<CatalogoResponse> listar() {
    return universidades.catalogo();
  }

  @GetMapping("/{id}")
  public UniversidadResponse consultar(@PathVariable Integer id) {
    return universidades.consultar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UniversidadResponse crear(
    @Valid @RequestBody UniversidadRequest request
  ) {
    return universidades.crear(request);
  }

  @PutMapping("/{id}")
  public UniversidadResponse actualizar(
    @PathVariable Integer id,
    @Valid @RequestBody UniversidadRequest request
  ) {
    return universidades.actualizar(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void desactivar(@PathVariable Integer id) {
    universidades.cambiarEstado(id, "Inactivo");
  }

  @PatchMapping("/{id}/estado")
  public UniversidadResponse cambiarEstado(
    @PathVariable Integer id,
    @Valid @RequestBody EstadoUniversidadRequest request
  ) {
    return universidades.cambiarEstado(id, request.estado());
  }

  @DeleteMapping("/{id}/definitivo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Integer id) {
    universidades.eliminarDefinitivamente(id);
  }

  @GetMapping("/{id}/programas")
  public PaginaResponse<Map<String, Object>> programas(
    @PathVariable Integer id,
    @RequestParam Map<String, String> filtros
  ) {
    return universidades.programas(id, filtros);
  }
}
