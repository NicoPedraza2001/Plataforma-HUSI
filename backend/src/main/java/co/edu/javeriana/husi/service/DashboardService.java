package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.response.DashboardResponse;
import co.edu.javeriana.husi.dto.response.RelacionFiltrosResponse;
import java.util.List;
import java.util.Map;

public interface DashboardService {
  DashboardResponse consultar(Map<String, String> filtros);

  /** Tablero del coordinador: ninguna cifra incluye residentes ni datos de otros servicios. */
  DashboardResponse consultarDelServicio(String servicio);

  List<RelacionFiltrosResponse> relaciones();
}
