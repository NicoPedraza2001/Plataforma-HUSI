package co.edu.javeriana.husi.controller;

import co.edu.javeriana.husi.dto.response.TipoDocumentoResponse;
import co.edu.javeriana.husi.service.TipoDocumentoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TipoDocumentoController {

  private final TipoDocumentoService tipos;

  @GetMapping("/tipos-documento")
  public List<TipoDocumentoResponse> listar() {
    return tipos.listar();
  }
}
