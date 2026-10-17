package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.response.ResidentesACargoResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.model.entity.Coordinador;
import co.edu.javeriana.husi.repository.CoordinadorRepository;
import co.edu.javeriana.husi.repository.ResidenteACargoRepository;
import co.edu.javeriana.husi.security.UsuarioEnSesion;
import co.edu.javeriana.husi.service.CoordinadorService;
import co.edu.javeriana.husi.util.Calendario;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoordinadorServiceImpl implements CoordinadorService {

  private final UsuarioEnSesion usuarioEnSesion;
  private final CoordinadorRepository coordinadorRepository;
  private final ResidenteACargoRepository residenteACargoRepository;

  @Override
  public ResidentesACargoResponse listarResidentes() {
    Coordinador coordinador = coordinacionActiva();
    Integer servicioId = coordinador.getServicio().getId();
    LocalDate hoy = Calendario.hoy();
    return new ResidentesACargoResponse(
      coordinador.getServicio().getNombre(),
      hoy,
      residenteACargoRepository.listarEnCurso(servicioId, hoy),
      residenteACargoRepository.listarProximas(servicioId, hoy)
    );
  }

  /** El servicio sale siempre de la sesión: un coordinador nunca elige qué servicio consulta. */
  private Coordinador coordinacionActiva() {
    Integer usuarioId = usuarioEnSesion.consultar().getId();
    return coordinadorRepository
      .findByUsuarioId(usuarioId)
      .filter(coordinador -> "Activo".equals(coordinador.getEstado()))
      .orElseThrow(() ->
        new NegocioException(
          HttpStatus.FORBIDDEN,
          "Tu usuario no tiene un servicio asignado. Pide al administrador que te asigne uno."
        )
      );
  }
}
