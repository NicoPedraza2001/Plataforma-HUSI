package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

  private final UsuarioService usuarioService;

  @GetMapping("/{id}")
  public UsuarioResponse consultar(@PathVariable Integer id) {
    return usuarioService.consultar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
    return usuarioService.crear(request);
  }

  @PutMapping("/{id}")
  public UsuarioResponse actualizar(
    @PathVariable Integer id,
    @Valid @RequestBody UsuarioRequest request
  ) {
    return usuarioService.actualizar(id, request);
  }

  @PatchMapping("/{id}/estado")
  public UsuarioResponse cambiarEstado(
    @PathVariable Integer id,
    @Valid @RequestBody EstadoRequest request
  ) {
    return usuarioService.cambiarEstado(id, request.estado());
  }

  @DeleteMapping("/{id}/definitivo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Integer id) {
    usuarioService.eliminarDefinitivamente(id);
  }
}
