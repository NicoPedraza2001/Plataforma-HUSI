package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.repository.DashboardRepository;
import co.edu.javeriana.husi.service.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(
  readOnly = true,
  isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
)
public class DashboardServiceImpl implements DashboardService {

  private final ConsultaService consultas;
  private final DashboardRepository repository;

  public Map<String, Object> consultar(Map<String, String> filtros) {
    consultas.validar("rotaciones", filtros);
    Map<String, String> normalizados = new HashMap<>();
    filtros.forEach((key, value) -> {
      if (!value.isBlank()) normalizados.put(key, value.strip());
    });
    return repository.consultar(normalizados);
  }
}
