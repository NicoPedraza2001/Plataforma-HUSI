package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.dto.response.DashboardResponse;
import co.edu.javeriana.husi.dto.response.DashboardResponse.Barra;
import co.edu.javeriana.husi.dto.response.DashboardResponse.Valor;
import co.edu.javeriana.husi.dto.response.ServicioResponse;
import co.edu.javeriana.husi.dto.request.ServicioRequest;
import co.edu.javeriana.husi.dto.response.ResidenteACargoResponse;
import co.edu.javeriana.husi.model.entity.RotacionId;
import co.edu.javeriana.husi.util.Calendario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** CU-12 Consultar residentes y tablero del servicio (RF-34, RNF-11). */
class CU12CoordinadorIntegrationTests extends BaseCasoDeUso {

  private final LocalDate hoy = Calendario.hoy();

  /** Los cambios hechos con SQL directo no se ven hasta vaciar las entidades ya cargadas. */
  @PersistenceContext
  EntityManager gestorDeEntidades;

  private RotacionId rotacionEnCurso(Integer servicioId) {
    return insertarRotacion(asignaturaDe(servicioId), hoy.minusDays(5), hoy.plusDays(5));
  }

  private String valorDe(DashboardResponse respuesta, String idIndicador) {
    return respuesta
      .indicadores()
      .stream()
      .filter(indicador -> indicador.id().equals(idIndicador))
      .findFirst()
      .orElseThrow()
      .valor();
  }

  /** Un residente asignado en una rotación en curso de cada servicio; actúa como su coordinador. */
  private void coordinarServicioConOtroAlLado(ServicioResponse propio, ServicioResponse ajeno) {
    asignaciones.asignar(rotacionEnCurso(propio.id()), residenteNuevo());
    asignaciones.asignar(rotacionEnCurso(ajeno.id()), residenteNuevo());
    actuarComo(coordinadorDe(propio.id()).correo());
  }

  @Test
  @DisplayName("CU12-FP2 El tablero presenta las cifras de su servicio")
  void elTableroPresentaLasCifrasDeSuServicio() {
    var servicio = servicioConCupo(3);
    asignarResidentesNuevos(rotacionEnCurso(servicio.id()), 2);
    actuarComo(coordinadorDe(servicio.id()).correo());

    var respuesta = coordinadores.dashboardPropio();

    assertThat(respuesta.totalRegistros()).isEqualTo(2);
    assertThat(respuesta.rotacionesPorServicio()).containsExactly(
      new Barra(servicio.nombre(), 1, 1)
    );
    assertThat(respuesta.mapaOcupacion().filas()).hasSize(1);
    assertThat(respuesta.mapaOcupacion().filas().getFirst().servicio()).isEqualTo(servicio.nombre());
  }

  @Test
  @DisplayName("CU12-FA1 Un servicio sin residentes devuelve listas vacías, sin errores")
  void unServicioSinResidentesDevuelveListasVacias() {
    var servicio = servicioConCupo(2);
    actuarComo(coordinadorDe(servicio.id()).correo());

    var panel = coordinadores.listarResidentes();
    var respuesta = coordinadores.dashboardPropio();

    assertThat(panel.servicio()).isEqualTo(servicio.nombre());
    assertThat(panel.residentes()).isEmpty();
    assertThat(panel.proximos()).isEmpty();
    assertThat(respuesta.totalRegistros()).isZero();
    assertThat(respuesta.mapaOcupacion().filas()).isEmpty();
  }

  @Test
  @DisplayName("CU12-FA2 Las rotaciones terminadas, inactivas o en espera no aparecen")
  void soloAparecenRotacionesVigentesYAsignadas() {
    var servicio = servicioConCupo(1);
    Integer asignatura = asignaturaDe(servicio.id());
    int enCurso = residenteNuevo();
    int enEspera = residenteNuevo();
    int deRotacionTerminada = residenteNuevo();
    int deRotacionInactiva = residenteNuevo();
    var vigente = insertarRotacion(asignatura, hoy.minusDays(5), hoy.plusDays(5));
    asignaciones.asignar(vigente, enCurso);
    asignaciones.asignar(vigente, enEspera);
    asignaciones.asignarHistorica(
      insertarRotacion(asignatura, hoy.minusDays(30), hoy.minusDays(20)),
      deRotacionTerminada
    );
    var inactiva = insertarRotacion(asignatura, hoy.minusDays(4), hoy.plusDays(6));
    asignaciones.asignar(inactiva, deRotacionInactiva);
    rotaciones.cambiarEstado(inactiva, false);
    actuarComo(coordinadorDe(servicio.id()).correo());

    var panel = coordinadores.listarResidentes();

    assertThat(panel.residentes())
      .extracting(ResidenteACargoResponse::residenteId)
      .containsExactly(enCurso);
    assertThat(panel.proximos()).isEmpty();
  }

  @Test
  @DisplayName("CU12-EX1 Aislamiento en residentes: nada de otros servicios")
  void losResidentesNoIncluyenOtrosServicios() {
    var propio = servicioConCupo(3);
    var ajeno = servicioConCupo(3);
    int residentePropio = residenteNuevo();
    int residenteAjeno = residenteNuevo();
    int proximoAjeno = residenteNuevo();
    asignaciones.asignar(rotacionEnCurso(propio.id()), residentePropio);
    asignaciones.asignar(rotacionEnCurso(ajeno.id()), residenteAjeno);
    asignaciones.asignar(
      crearRotacion(asignaturaDe(ajeno.id()), INICIO_FUTURO, FIN_FUTURO),
      proximoAjeno
    );
    actuarComo(coordinadorDe(propio.id()).correo());

    var panel = coordinadores.listarResidentes();

    assertThat(panel.residentes())
      .extracting(ResidenteACargoResponse::residenteId)
      .containsExactly(residentePropio);
    assertThat(panel.proximos()).isEmpty();
  }

  @Test
  @DisplayName("CU12-EX2 Aislamiento en tablero: las rotaciones y la ocupación son solo de su servicio")
  void elTableroNoIncluyeRotacionesDeOtrosServicios() {
    var propio = servicioConCupo(3);
    var ajeno = servicioConCupo(3);
    coordinarServicioConOtroAlLado(propio, ajeno);

    var respuesta = coordinadores.dashboardPropio();

    assertThat(respuesta.totalRegistros()).isEqualTo(1);
    assertThat(respuesta.rotacionesPorServicio())
      .extracting(Barra::etiqueta)
      .containsExactly(propio.nombre());
    assertThat(respuesta.mapaOcupacion().filas())
      .extracting("servicio")
      .containsExactly(propio.nombre());
    assertThat(respuesta.distribucionUniversidad()).hasSize(1);
  }

  @Test
  @DisplayName("CU12-EX2 Aislamiento en tablero: los residentes por programa son solo de su servicio")
  void elTableroNoIncluyeResidentesDeOtrosServicios() {
    var propio = servicioConCupo(3);
    var ajeno = servicioConCupo(3);
    coordinarServicioConOtroAlLado(propio, ajeno);

    var respuesta = coordinadores.dashboardPropio();

    assertThat(valorDe(respuesta, "residentes")).isEqualTo("1");
    assertThat(respuesta.situacionPorPrograma()).hasSize(1);
    assertThat(respuesta.cargaResidentes()).contains(new Valor("1 rotación", 1));
    assertThat(respuesta.cargaResidentes().stream().mapToLong(Valor::valor).sum()).isEqualTo(1);
    assertThat(respuesta.alertas()).extracting("id").doesNotContain("sin-coordinador");
  }

  @Test
  @DisplayName("CU10 El administrador sí ve a los residentes de todo el periodo al filtrar un servicio")
  void elAdministradorMideAlResidenteEnTodoElPeriodo() {
    var propio = servicioConCupo(3);
    var ajeno = servicioConCupo(3);
    asignaciones.asignar(rotacionEnCurso(propio.id()), residenteNuevo());
    asignaciones.asignar(rotacionEnCurso(ajeno.id()), residenteNuevo());

    var todos = tablero.consultar(java.util.Map.of());
    var conFiltro = tablero.consultar(java.util.Map.of("servicio", propio.nombre()));

    assertThat(valorDe(conFiltro, "residentes")).isEqualTo(valorDe(todos, "residentes"));
    assertThat(conFiltro.totalRegistros()).isEqualTo(1);
  }

  @Test
  @DisplayName("CU12-EX2 Aislamiento en tablero: un servicio de nombre parecido no se mezcla")
  void unServicioDeNombreParecidoNoSeMezcla() {
    var propio = servicioConCupo(3);
    var parecido = servicios.crear(
      new ServicioRequest(propio.nombre() + " Sede Norte", null, 3, 10)
    );
    coordinarServicioConOtroAlLado(propio, parecido);

    var respuesta = coordinadores.dashboardPropio();

    assertThat(respuesta.totalRegistros()).isEqualTo(1);
    assertThat(respuesta.rotacionesPorServicio())
      .extracting(Barra::etiqueta)
      .containsExactly(propio.nombre());
  }

  @Test
  @DisplayName("CU12-EX4 Sin vínculo activo con un servicio responde 403 con mensaje")
  void sinVinculoActivoResponde403() {
    var coordinador = coordinadorDe(servicioConCupo(2).id());
    jdbc.update("update coordinador set estado='Inactivo' where id_usuario=?", coordinador.id());
    gestorDeEntidades.clear();
    actuarComo(coordinador.correo());

    debeFallar(HttpStatus.FORBIDDEN, "no tiene un servicio asignado", () ->
      coordinadores.listarResidentes()
    );
    debeFallar(HttpStatus.FORBIDDEN, "no tiene un servicio asignado", () ->
      coordinadores.dashboardPropio()
    );

    jdbc.update("delete from coordinador where id_usuario=?", coordinador.id());
    gestorDeEntidades.clear();
    debeFallar(HttpStatus.FORBIDDEN, "no tiene un servicio asignado", () ->
      coordinadores.listarResidentes()
    );
  }
}
