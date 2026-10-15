package co.edu.javeriana.husi.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Panel del coordinador: su servicio y los residentes que rotan en él a la fecha de corte
 * ({@code fecha}, hoy en Bogotá). Las rotaciones finalizadas o futuras no se incluyen.
 */
public record ResidentesACargoResponse(
  String servicio,
  LocalDate fecha,
  List<ResidenteACargoResponse> residentes
) {}
