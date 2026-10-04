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

  private final PerfilAcademicoRepository perfilAcademicoRepository;

  public List<CatalogoResponse> listar() {
    return perfilAcademicoRepository
      .findAllByOrderByNombre()
      .stream()
      .map(perfil -> new CatalogoResponse(perfil.getId(), perfil.getNombre()))
      .toList();
  }
}
