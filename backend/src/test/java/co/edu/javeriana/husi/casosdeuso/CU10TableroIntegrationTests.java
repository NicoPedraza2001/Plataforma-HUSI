package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.controller.DashboardController;
import co.edu.javeriana.husi.dto.response.DashboardResponse;
import co.edu.javeriana.husi.dto.response.DashboardResponse.Alerta;
import co.edu.javeriana.husi.dto.response.DashboardResponse.Barra;
import co.edu.javeriana.husi.dto.response.DashboardResponse.Indicador;
import co.edu.javeriana.husi.dto.response.DashboardResponse.Porcentaje;
import co.edu.javeriana.husi.dto.response.DashboardResponse.Valor;
import co.edu.javeriana.husi.dto.response.RelacionFiltrosResponse;
import co.edu.javeriana.husi.model.entity.RotacionId;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * CU-10 Consultar el tablero (RF-30 a RF-32). Los datos se crean en un semestre lejano, sin datos
 * reales, para que cada cifra se pueda comprobar exactamente.
 */
class CU10TableroIntegrationTests extends BaseCasoDeUso {

  private static final String PERIODO = "2093-1";
  private static final String PERIODO_SIN_DATOS = "2094-1";

  @Autowired
  DashboardController controladorDelTablero;

  /**
   * Servicio con cupo 1 y dos rotaciones que se cruzan del 15 al 31 de enero. Queda: 1 residente
   * asignado en cada rotación y 1 en lista de espera, los tres del mismo programa.
   */
  private record Escenario(
    String servicio,
    Integer asignaturaId,
    ProgramaDePrueba programa,
    RotacionId primeraRotacion,
    int residenteEnEspera
  ) {}

  private Escenario crearEscenario() {
    var servicio = servicioConCupo(1);
    Integer asignatura = asignaturaDe(servicio.id());
    var programa = programaNuevo();
    var primera = crearRotacion(asignatura, LocalDate.of(2093, 1, 1), LocalDate.of(2093, 1, 31));
    var segunda = crearRotacion(asignatura, LocalDate.of(2093, 1, 15), LocalDate.of(2093, 2, 15));
    asignaciones.asignar(primera, residenteDelPrograma(programa.id()));
    asignaciones.asignar(segunda, residenteDelPrograma(programa.id()));
    int enEspera = residenteDelPrograma(programa.id());
    asignaciones.asignar(primera, enEspera);
    return new Escenario(servicio.nombre(), asignatura, programa, primera, enEspera);
  }

  private DashboardResponse tableroDelPeriodo() {
    return tablero.consultar(Map.of("periodo", PERIODO));
  }

  private String valorDe(DashboardResponse respuesta, String idIndicador) {
    return respuesta
      .indicadores()
      .stream()
      .filter(indicador -> indicador.id().equals(idIndicador))
      .map(Indicador::valor)
      .findFirst()
      .orElseThrow();
  }

  private Optional<Alerta> alertaDe(DashboardResponse respuesta, String idAlerta) {
    return respuesta.alertas().stream().filter(alerta -> alerta.id().equals(idAlerta)).findFirst();
  }

  private long cantidadSinCoordinador() {
    return alertaDe(tablero.consultar(Map.of("periodo", PERIODO)), "sin-coordinador")
      .map(Alerta::cantidad)
      .orElse(0L);
  }

  @Test
  @DisplayName("CU10-FP1 Sin filtros, el tablero suma exactamente lo que se registró")
  void sinFiltrosElTableroSumaLoRegistrado() {
    var antes = tablero.consultar(Map.of());
    crearEscenario();
    var despues = tablero.consultar(Map.of());

    assertThat(despues.totalRegistros()).isEqualTo(antes.totalRegistros() + 3);
    assertThat(Long.parseLong(valorDe(despues, "residentes"))).isEqualTo(
      Long.parseLong(valorDe(antes, "residentes")) + 3
    );
    assertThat(Long.parseLong(valorDe(despues, "rotaciones"))).isEqualTo(
      Long.parseLong(valorDe(antes, "rotaciones")) + 2
    );
  }

  @Test
  @DisplayName("CU10-FP2 Por periodo, los indicadores coinciden con los datos preparados")
  void porPeriodoLosIndicadoresCoinciden() {
    var escenario = crearEscenario();

    var respuesta = tableroDelPeriodo();

    assertThat(respuesta.totalRegistros()).isEqualTo(3);
    assertThat(valorDe(respuesta, "residentes")).isEqualTo("3");
    assertThat(valorDe(respuesta, "con-rotacion")).isEqualTo("2");
    assertThat(valorDe(respuesta, "rotaciones")).isEqualTo("2");
    assertThat(respuesta.situacionPorPrograma()).hasSize(1);
    assertThat(respuesta.situacionPorPrograma().getFirst().etiqueta()).isEqualTo(
      escenario.programa().nombre()
    );
    assertThat(respuesta.situacionPorPrograma().getFirst().conRotacion()).isEqualTo(2);
    assertThat(respuesta.situacionPorPrograma().getFirst().enEspera()).isEqualTo(1);
    assertThat(respuesta.rotacionesPorServicio()).containsExactly(
      new Barra(escenario.servicio(), 2, 2)
    );
    assertThat(respuesta.tendenciaInicios()).containsExactly(new Valor("2093-01", 2));
    assertThat(respuesta.mapaOcupacion().meses()).hasSize(6);
  }

  @Test
  @DisplayName("CU10-FP2 Lo que inicia en otro semestre no se cuenta en el periodo")
  void loDeOtroSemestreNoSeCuenta() {
    crearEscenario();
    assertThat(tablero.consultar(Map.of("periodo", "2093-2")).totalRegistros()).isZero();
  }

  @Test
  @DisplayName("CU10-FA1 Cada filtro recorta los resultados a su universidad, programa, servicio o asignatura")
  void cadaFiltroRecortaLosResultados() {
    var buscado = crearEscenario();
    crearEscenario();
    assertThat(tableroDelPeriodo().totalRegistros()).isEqualTo(6);

    for (Map<String, String> filtro : java.util.List.of(
      Map.of("universidad", buscado.programa().universidad()),
      Map.of("programa", buscado.programa().nombre()),
      Map.of("servicio", buscado.servicio()),
      Map.of("asignaturaId", String.valueOf(buscado.asignaturaId()))
    )) {
      var filtros = new java.util.HashMap<>(filtro);
      filtros.put("periodo", PERIODO);
      assertThat(tablero.consultar(filtros).totalRegistros()).as(filtro.toString()).isEqualTo(3);
    }
  }

  @Test
  @DisplayName("CU10-FA2 Las relaciones encadenan los filtros: un servicio solo muestra sus asignaturas")
  void lasRelacionesEncadenanLosFiltros() {
    var escenario = crearEscenario();
    var otro = crearEscenario();

    var relaciones = tablero.relaciones();

    assertThat(relaciones).contains(
      new RelacionFiltrosResponse(
        escenario.programa().nombre(),
        escenario.servicio(),
        escenario.asignaturaId()
      )
    );
    assertThat(relaciones)
      .filteredOn(relacion -> relacion.servicio().equals(escenario.servicio()))
      .extracting(RelacionFiltrosResponse::asignaturaId)
      .containsOnly(escenario.asignaturaId())
      .doesNotContain(otro.asignaturaId());
  }

  @Test
  @DisplayName("CU10-FA3 Un periodo sin datos devuelve un panel vacío, sin errores")
  void unPeriodoSinDatosDevuelveUnPanelVacio() {
    var respuesta = tablero.consultar(Map.of("periodo", PERIODO_SIN_DATOS));

    assertThat(respuesta.totalRegistros()).isZero();
    assertThat(valorDe(respuesta, "residentes")).isEqualTo("0");
    assertThat(valorDe(respuesta, "ocupacion")).isEqualTo("—");
    assertThat(respuesta.situacionPorPrograma()).isEmpty();
    assertThat(respuesta.revisar()).isEmpty();
    assertThat(respuesta.distribucionUniversidad()).isEmpty();
    assertThat(respuesta.mapaOcupacion().filas()).isEmpty();
  }

  @Test
  @DisplayName("CU10-FA4 Alerta de residentes con fechas cruzadas; las consecutivas no cuentan")
  void alertaDeFechasCruzadas() {
    Integer asignatura = asignaturaConCupo(5);
    int conCruce = residenteNuevo();
    int consecutivo = residenteNuevo();
    var enero = crearRotacion(asignatura, LocalDate.of(2093, 1, 1), LocalDate.of(2093, 1, 31));
    var cruzada = crearRotacion(asignatura, LocalDate.of(2093, 1, 20), LocalDate.of(2093, 2, 28));
    var marzo = crearRotacion(asignatura, LocalDate.of(2093, 3, 1), LocalDate.of(2093, 3, 31));
    var abril = crearRotacion(asignatura, LocalDate.of(2093, 4, 1), LocalDate.of(2093, 4, 30));
    asignaciones.asignar(enero, conCruce);
    asignaciones.asignar(cruzada, conCruce);
    asignaciones.asignar(marzo, consecutivo);
    asignaciones.asignar(abril, consecutivo);

    var respuesta = tableroDelPeriodo();

    assertThat(alertaDe(respuesta, "cruce")).get().extracting(Alerta::cantidad).isEqualTo(1L);
    assertThat(valorDe(respuesta, "cruces")).isEqualTo("1");
    assertThat(respuesta.revisar())
      .filteredOn(revisar -> revisar.motivo().equals("cruce"))
      .hasSize(1)
      .first()
      .satisfies(revisar -> {
        assertThat(revisar.residenteId()).isEqualTo(conCruce);
        assertThat(revisar.detalle()).contains("del 2093-01-20 al 2093-01-31, 12 días");
      });
  }

  @Test
  @DisplayName("CU10-FA5 Alerta de residentes en lista de espera")
  void alertaDeListaDeEspera() {
    var escenario = crearEscenario();

    var respuesta = tableroDelPeriodo();

    assertThat(alertaDe(respuesta, "espera")).get().extracting(Alerta::cantidad).isEqualTo(1L);
    assertThat(respuesta.revisar())
      .filteredOn(revisar -> revisar.motivo().equals("espera"))
      .extracting("residenteId")
      .containsExactly(escenario.residenteEnEspera());
  }

  @Test
  @DisplayName("CU10-FA6 Alerta de servicios por encima de su cupo")
  void alertaDeServiciosSobreCupo() {
    var escenario = crearEscenario();

    var respuesta = tableroDelPeriodo();

    assertThat(alertaDe(respuesta, "sobre-cupo")).get().extracting(Alerta::cantidad).isEqualTo(1L);
    assertThat(valorDe(respuesta, "ocupacion")).isEqualTo("200%");
    var fila = respuesta.mapaOcupacion().filas().getFirst();
    assertThat(fila.servicio()).isEqualTo(escenario.servicio());
    assertThat(fila.diasExceso()).isEqualTo(17);
    assertThat(fila.maximo()).isEqualTo(200);
  }

  @Test
  @DisplayName("CU10-FA7 Alerta de servicios activos sin coordinador activo")
  void alertaDeServiciosSinCoordinador() {
    long antes = cantidadSinCoordinador();

    var servicio = servicioConCupo(1);
    assertThat(cantidadSinCoordinador()).isEqualTo(antes + 1);

    var coordinador = coordinadorDe(servicio.id());
    assertThat(cantidadSinCoordinador()).isEqualTo(antes);

    usuarios.cambiarEstado(coordinador.id(), "Inactivo");
    assertThat(cantidadSinCoordinador()).isEqualTo(antes + 1);
  }

  @Test
  @DisplayName("CU10-FA8 Sin cruces, esperas ni exceso de cupo no aparecen esas alertas")
  void sinSituacionesNoAparecenAlertas() {
    var rotacion = crearRotacion(
      asignaturaConCupo(5),
      LocalDate.of(2093, 1, 1),
      LocalDate.of(2093, 1, 31)
    );
    asignaciones.asignar(rotacion, residenteNuevo());

    var respuesta = tableroDelPeriodo();

    assertThat(respuesta.alertas())
      .extracting(Alerta::id)
      .doesNotContain("cruce", "espera", "sobre-cupo");
    assertThat(respuesta.revisar()).isEmpty();
    assertThat(valorDe(respuesta, "con-rotacion")).isEqualTo("1");
  }

  @Test
  @DisplayName("CU10-FA9 La carga agrupa desde 6 rotaciones")
  void laCargaAgrupaDesdeSeisRotaciones() {
    var servicio = servicioConCupo(5);
    Integer asignatura = asignaturaDe(servicio.id());
    int residente = residenteNuevo();
    for (int mes = 1; mes <= 6; mes++) asignaciones.asignar(
      crearRotacion(asignatura, LocalDate.of(2093, mes, 1), LocalDate.of(2093, mes, 10)),
      residente
    );

    var respuesta = tablero.consultar(Map.of("periodo", PERIODO, "servicio", servicio.nombre()));

    assertThat(respuesta.cargaResidentes()).contains(new Valor("6 o más", 1));
    assertThat(respuesta.cargaResidentes()).contains(new Valor("1 rotación", 0));
  }

  @Test
  @DisplayName("CU10-FA9 La distribución muestra tres universidades y agrupa el resto en «Otras»")
  void laDistribucionAgrupaElRestoEnOtras() {
    var servicio = servicioConCupo(5);
    var rotacion = crearRotacion(
      asignaturaDe(servicio.id()),
      LocalDate.of(2093, 1, 1),
      LocalDate.of(2093, 1, 31)
    );
    asignarResidentesNuevos(rotacion, 5);

    var distribucion = tablero
      .consultar(Map.of("periodo", PERIODO, "servicio", servicio.nombre()))
      .distribucionUniversidad();

    assertThat(distribucion).hasSize(4);
    assertThat(distribucion.getLast()).isEqualTo(new Porcentaje("Otras", 40.0));
    assertThat(distribucion).extracting(Porcentaje::porcentaje).containsExactly(20.0, 20.0, 20.0, 40.0);
  }

  @Test
  @DisplayName("CU10-EX1 Un filtro desconocido responde 400")
  void unFiltroDesconocidoResponde400() {
    debeFallar(HttpStatus.BAD_REQUEST, "no disponible", () ->
      controladorDelTablero.consultar(Map.of("noExiste", "x"))
    );
  }

  @Test
  @DisplayName("CU10-EX2 Un periodo mal formado o una asignatura no numérica responden 400")
  void losFiltrosMalFormadosResponden400() {
    debeFallar(HttpStatus.BAD_REQUEST, "formato AAAA-1 o AAAA-2", () ->
      tablero.consultar(Map.of("periodo", "2093-3"))
    );
    debeFallar(HttpStatus.BAD_REQUEST, "entero positivo", () ->
      tablero.consultar(Map.of("asignaturaId", "abc"))
    );
    debeFallar(HttpStatus.BAD_REQUEST, "entero positivo", () ->
      tablero.consultar(Map.of("asignaturaId", "-1"))
    );
  }
}
