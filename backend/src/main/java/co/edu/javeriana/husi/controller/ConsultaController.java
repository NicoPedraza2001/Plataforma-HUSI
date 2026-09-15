package co.edu.javeriana.husi.controller;
import co.edu.javeriana.husi.service.ConsultaService;
import co.edu.javeriana.husi.dto.response.PaginaResponse;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.*;
@RestController @RequiredArgsConstructor
public class ConsultaController {
 private final ConsultaService consultas;
 @GetMapping({"/residentes","/servicios","/usuarios","/historial","/rotaciones","/programas/buscar","/universidades/buscar"})
 public PaginaResponse<Map<String,Object>> listar(jakarta.servlet.http.HttpServletRequest request,@RequestParam Map<String,String> parametros) {
  String dominio=request.getServletPath().split("/")[1];
  return consultas.listar(dominio,parametros);
 }
 @GetMapping("/catalogos") public Map<String,List<String>> catalogos() { return consultas.catalogos(); }
}
