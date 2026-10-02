package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.response.TipoDocumentoResponse;
import co.edu.javeriana.husi.repository.TipoDocumentoRepository;
import co.edu.javeriana.husi.service.TipoDocumentoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TipoDocumentoServiceImpl implements TipoDocumentoService {

  private final TipoDocumentoRepository tipos;

  public List<TipoDocumentoResponse> listar() {
    return tipos
      .findAllByOrderByCodigo()
      .stream()
      .map(t -> new TipoDocumentoResponse(t.getId(), t.getCodigo(), t.getNombre()))
      .toList();
  }
}
