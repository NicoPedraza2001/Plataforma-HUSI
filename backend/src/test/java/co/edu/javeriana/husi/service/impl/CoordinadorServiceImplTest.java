package co.edu.javeriana.husi.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.model.entity.Coordinador;
import co.edu.javeriana.husi.model.entity.Servicio;
import co.edu.javeriana.husi.model.entity.Usuario;
import co.edu.javeriana.husi.repository.CoordinadorRepository;
import co.edu.javeriana.husi.repository.ResidenteACargoRepository;
import co.edu.javeriana.husi.security.UsuarioEnSesion;
import co.edu.javeriana.husi.service.DashboardService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Prueba unitaria: el coordinador nunca elige qué servicio consulta (RF-34, RNF-11). */
class CoordinadorServiceImplTest {

  private static final int ID_USUARIO = 7;

  private final UsuarioEnSesion usuarioEnSesion = mock(UsuarioEnSesion.class);
  private final CoordinadorRepository coordinadorRepository = mock(CoordinadorRepository.class);
  private final ResidenteACargoRepository residenteACargoRepository = mock(
    ResidenteACargoRepository.class
  );
  private final DashboardService dashboardService = mock(DashboardService.class);
  private final CoordinadorServiceImpl servicio = new CoordinadorServiceImpl(
    usuarioEnSesion,
    coordinadorRepository,
    residenteACargoRepository,
    dashboardService
  );

  @BeforeEach
  void iniciarSesion() {
    var usuario = new Usuario();
    usuario.setId(ID_USUARIO);
    when(usuarioEnSesion.consultar()).thenReturn(usuario);
  }

  private void conVinculo(String estado) {
    var servicioCoordinado = new Servicio();
    servicioCoordinado.setNombre("Cirugía");
    var coordinador = new Coordinador();
    coordinador.setServicio(servicioCoordinado);
    coordinador.setEstado(estado);
    when(coordinadorRepository.findByUsuarioId(ID_USUARIO)).thenReturn(Optional.of(coordinador));
  }

  @Test
  @DisplayName("CU12-EX3 El tablero se pide solo con el servicio de la sesión")
  void elTableroSePideConElServicioDeLaSesion() {
    conVinculo("Activo");

    servicio.dashboardPropio();

    verify(dashboardService).consultarDelServicio("Cirugía");
  }

  @Test
  @DisplayName("CU12-EX4 Un vínculo inactivo no consulta nada y responde 403")
  void unVinculoInactivoNoConsultaNada() {
    conVinculo("Inactivo");

    assertThatThrownBy(servicio::dashboardPropio).isInstanceOf(NegocioException.class);
    assertThatThrownBy(servicio::listarResidentes).isInstanceOf(NegocioException.class);

    verifyNoInteractions(dashboardService, residenteACargoRepository);
  }

  @Test
  @DisplayName("CU12-EX4 Sin vínculo no consulta nada y responde 403")
  void sinVinculoNoConsultaNada() {
    when(coordinadorRepository.findByUsuarioId(ID_USUARIO)).thenReturn(Optional.empty());

    assertThatThrownBy(servicio::dashboardPropio).isInstanceOf(NegocioException.class);

    verifyNoInteractions(dashboardService);
  }
}
