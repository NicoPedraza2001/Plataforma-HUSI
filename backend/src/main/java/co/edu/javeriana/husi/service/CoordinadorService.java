package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.response.ResidentesACargoResponse;

public interface CoordinadorService {
  /** Residentes que rotan hoy y los que llegarán al servicio del coordinador en sesión. */
  ResidentesACargoResponse listarResidentes();
}
