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

  private final TipoDocumentoRepository tipoDocumentoRepository;

  public List<TipoDocumentoResponse> listar() {
    return tipoDocumentoRepository
      .findAllByOrderByCodigo()
      .stream()
      .map(tipo ->
        new TipoDocumentoResponse(tipo.getId(), tipo.getCodigo(), tipo.getNombre())
      )
      .toList();
  }
}
