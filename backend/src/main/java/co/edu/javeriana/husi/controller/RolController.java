package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.response.CatalogoResponse;
import co.edu.javeriana.husi.service.RolService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RolController {

  private final RolService roles;

  @GetMapping("/roles")
  public List<CatalogoResponse> listar() {
    return roles.listar();
  }
}
