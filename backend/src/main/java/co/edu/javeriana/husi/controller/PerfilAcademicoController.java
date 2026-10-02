package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.response.CatalogoResponse;
import co.edu.javeriana.husi.service.PerfilAcademicoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PerfilAcademicoController {

  private final PerfilAcademicoService perfiles;

  @GetMapping("/perfiles-academicos")
  public List<CatalogoResponse> listar() {
    return perfiles.listar();
  }
}
