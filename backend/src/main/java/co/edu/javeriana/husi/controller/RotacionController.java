package co.edu.javeriana.husi.controller;
import co.edu.javeriana.husi.service.*;
import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.PaginaResponse;
import co.edu.javeriana.husi.model.entity.RotacionId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/rotaciones/gestion") @RequiredArgsConstructor
public class RotacionController {
 private final RotacionService rotaciones;
 private final ImportacionRotacionesService importacion;
 @GetMapping public PaginaResponse<Map<String,Object>> listar(@RequestParam Map<String,String> filtros) { return rotaciones.listar(filtros); }
 @GetMapping("/catalogos") public Map<String,Object> catalogos() { return rotaciones.catalogos(); }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> crear(@Valid @RequestBody RotacionRequest d) { return rotaciones.crear(d); }
 @GetMapping("/{asignatura}/{inicio}/{fin}") public Map<String,Object> consultar(@PathVariable Integer asignatura,@PathVariable LocalDate inicio,@PathVariable LocalDate fin) { return rotaciones.consultar(new RotacionId(asignatura,inicio,fin)); }
 @PutMapping("/{asignatura}/{inicio}/{fin}") public Map<String,Object> editar(@PathVariable Integer asignatura,@PathVariable LocalDate inicio,@PathVariable LocalDate fin,@Valid @RequestBody RotacionRequest d) { return rotaciones.editar(new RotacionId(asignatura,inicio,fin),d); }
 @PatchMapping("/{asignatura}/{inicio}/{fin}/estado") public Map<String,Object> estado(@PathVariable Integer asignatura,@PathVariable LocalDate inicio,@PathVariable LocalDate fin,@Valid @RequestBody EstadoRotacionRequest d) { return rotaciones.estado(new RotacionId(asignatura,inicio,fin),d.activa()); }
 @DeleteMapping("/{asignatura}/{inicio}/{fin}") @ResponseStatus(HttpStatus.NO_CONTENT) public void eliminar(@PathVariable Integer asignatura,@PathVariable LocalDate inicio,@PathVariable LocalDate fin) { rotaciones.eliminar(new RotacionId(asignatura,inicio,fin)); }
 @PostMapping("/importar/validar") public Map<String,Object> validar(@Valid @RequestBody ImportacionRotacionesRequest d) { return importacion.procesar(d,false); }
 @PostMapping("/importar") public Map<String,Object> importar(@Valid @RequestBody ImportacionRotacionesRequest d) { return importacion.procesar(d,true); }
}
