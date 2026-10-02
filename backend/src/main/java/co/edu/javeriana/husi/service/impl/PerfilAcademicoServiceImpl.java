package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.response.CatalogoResponse;
import co.edu.javeriana.husi.repository.PerfilAcademicoRepository;
import co.edu.javeriana.husi.service.PerfilAcademicoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerfilAcademicoServiceImpl implements PerfilAcademicoService {

  private final PerfilAcademicoRepository perfiles;

  public List<CatalogoResponse> listar() {
    return perfiles
      .findAllByOrderByNombre()
      .stream()
      .map(p -> new CatalogoResponse(p.getId(), p.getNombre()))
      .toList();
  }
}
