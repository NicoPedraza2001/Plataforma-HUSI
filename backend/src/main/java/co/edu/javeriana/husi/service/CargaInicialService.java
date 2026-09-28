package co.edu.javeriana.husi.service;

import co.edu.javeriana.husi.dto.request.CargaInicialRequest;
import co.edu.javeriana.husi.dto.response.CargaInicialResponse;

public interface CargaInicialService {
  CargaInicialResponse validar(CargaInicialRequest datos);
  CargaInicialResponse confirmar(CargaInicialRequest datos);
}
