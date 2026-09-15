package co.edu.javeriana.husi.controller;
import co.edu.javeriana.husi.service.*;
import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.util.List;
@RestController @RequestMapping("/programas") @RequiredArgsConstructor
public class ProgramaController {
 private final ProgramaService programas;
 private final ImportacionProgramasService importacion;
 @GetMapping public List<ProgramaResponse> listar() { return programas.listar(); }
 @GetMapping("/{id}") public ProgramaResponse consultar(@PathVariable Integer id) { return programas.consultar(id); }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProgramaResponse crear(@Valid @RequestBody ProgramaRequest request) { return programas.crear(request); }
 @PutMapping("/{id}") public ProgramaResponse actualizar(@PathVariable Integer id,@Valid @RequestBody ProgramaRequest request) { return programas.actualizar(id,request); }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void desactivar(@PathVariable Integer id) { programas.desactivar(id); }
 @PatchMapping("/{id}/estado") public ProgramaResponse estado(@PathVariable Integer id,@Valid @RequestBody EstadoProgramaRequest request) { return programas.cambiarEstado(id,request.estado()); }
 @DeleteMapping("/{id}/definitivo") @ResponseStatus(HttpStatus.NO_CONTENT) public void eliminar(@PathVariable Integer id) { programas.eliminarDefinitivamente(id); }
 @PostMapping("/importacion/validar") public ImportacionProgramasResponse validar(@Valid @RequestBody ImportacionProgramasRequest request) { return importacion.procesar(request,false); }
 @PostMapping("/importacion") public ImportacionProgramasResponse importar(@Valid @RequestBody ImportacionProgramasRequest request) { return importacion.procesar(request,true); }
}
