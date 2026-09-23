package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.response.CatalogoResponse;
import co.edu.javeriana.husi.repository.RolRepository;
import co.edu.javeriana.husi.service.RolService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RolServiceImpl implements RolService {

  private final RolRepository roles;

  public List<CatalogoResponse> listar() {
    return roles.listar();
  }
}
