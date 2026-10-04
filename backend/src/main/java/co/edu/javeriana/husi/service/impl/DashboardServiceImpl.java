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

  private final ConsultaService consultaService;
  private final DashboardRepository dashboardRepository;

  public Map<String, Object> consultar(Map<String, String> filtros) {
    consultaService.validar("rotaciones", filtros);
    Map<String, String> filtrosLimpios = new HashMap<>();
    filtros.forEach((nombre, valor) -> {
      if (!valor.isBlank()) filtrosLimpios.put(nombre, valor.strip());
    });
    return dashboardRepository.consultar(filtrosLimpios);
  }
}
