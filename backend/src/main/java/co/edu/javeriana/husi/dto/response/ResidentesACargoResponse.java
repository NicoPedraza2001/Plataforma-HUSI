package co.edu.javeriana.husi.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Panel del coordinador: su servicio, los residentes que rotan en él a la fecha de corte
 * ({@code fecha}, hoy en Bogotá) y las rotaciones que empiezan después ({@code proximos}).
 * Las rotaciones finalizadas no se incluyen.
 */
public record ResidentesACargoResponse(
  String servicio,
  LocalDate fecha,
  List<ResidenteACargoResponse> residentes,
  List<ResidenteACargoResponse> proximos
) {}
