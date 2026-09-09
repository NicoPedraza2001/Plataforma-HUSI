package co.edu.javeriana.husi.controller;
import co.edu.javeriana.husi.service.ProgramaService;
import co.edu.javeriana.husi.dto.response.CatalogoResponse;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;
@RestController @RequestMapping("/universidades") @RequiredArgsConstructor
public class UniversidadController {
 private final ProgramaService programas;
 @GetMapping public List<CatalogoResponse> listar() { return programas.universidades(); }
}
