package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.request.DashboardFiltros;
import co.edu.javeriana.husi.dto.response.DashboardResponse;
import co.edu.javeriana.husi.dto.response.DashboardResponse.*;
import co.edu.javeriana.husi.dto.response.RelacionFiltrosResponse;
import co.edu.javeriana.husi.repository.DashboardRepository;
import co.edu.javeriana.husi.repository.DashboardRepository.Conteo;
import co.edu.javeriana.husi.repository.DashboardRepository.Totales;
import co.edu.javeriana.husi.service.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arma el panel a partir de las lecturas del repositorio: cifras, textos y avisos. */
@Service
@RequiredArgsConstructor
@Transactional(
  readOnly = true,
  isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
)
public class DashboardServiceImpl implements DashboardService {

  /** La carga agrupa desde este número de rotaciones en adelante. */
  private static final int TOPE_CARGA = 6;
  /** Universidades que se muestran por nombre; el resto se agrupa en «Otras». */
  private static final int UNIVERSIDADES_VISIBLES = 3;
  private static final String[] MESES = {
    "ene",
    "feb",
    "mar",
    "abr",
    "may",
    "jun",
    "jul",
    "ago",
    "sep",
    "oct",
    "nov",
    "dic",
  };

  private final ConsultaService consultaService;
  private final DashboardRepository dashboardRepository;

  public DashboardResponse consultar(Map<String, String> parametros) {
    consultaService.validar("rotaciones", parametros);
    var filtros = DashboardFiltros.de(parametros);
    String alcance =
      filtros.periodo() == null
        ? "en todo el histórico"
        : "en " + filtros.periodo();

    var totales = dashboardRepository.totales(filtros);
    var situacion = dashboardRepository.situacionPorPrograma(filtros);
    var mapa = mapaOcupacion(filtros);
    var cruces = CrucesDashboard.porResidente(
      dashboardRepository.paresCruzados(filtros)
    );
    var enEspera = dashboardRepository.residentesEnEspera(filtros);
    List<ResidenteRevisar> revisar = new ArrayList<>(cruces);
    revisar.addAll(enEspera);

    return new DashboardResponse(
      indicadores(totales, situacion, cruces, mapa, alcance),
      situacion,
      carga(dashboardRepository.residentesPorCantidadDeRotaciones(filtros)),
      mapa,
      barras(dashboardRepository.rotacionesPorServicio(filtros)),
      distribucion(dashboardRepository.residentesPorUniversidad(filtros)),
      valores(dashboardRepository.iniciosPorMes(filtros)),
      alertas(cruces, enEspera, mapa, alcance),
      revisar,
      totales.total()
    );
  }

  public List<RelacionFiltrosResponse> relaciones() {
    return dashboardRepository.relaciones();
  }

  private MapaOcupacion mapaOcupacion(DashboardFiltros filtros) {
    return OcupacionDashboard.ventana(
      filtros.periodo(),
      dashboardRepository.rangoAsignaciones()
    )
      .map(ventana ->
        OcupacionDashboard.calcular(
          ventana,
          dashboardRepository.tramosOcupacion(
            filtros,
            ventana.desde(),
            ventana.hasta()
          )
        )
      )
      .orElse(MapaOcupacion.VACIO);
  }

  private List<Indicador> indicadores(
    Totales totales,
    List<SituacionPrograma> situacion,
    List<ResidenteRevisar> cruces,
    MapaOcupacion mapa,
    String alcance
  ) {
    long conRotacion = situacion
      .stream()
      .mapToLong(SituacionPrograma::conRotacion)
      .sum();
    long enEspera = situacion
      .stream()
      .mapToLong(SituacionPrograma::enEspera)
      .sum();
    long residentes = conRotacion + enEspera;
    int programas = situacion.size();

    return List.of(
      new Indicador(
        "residentes",
        "Residentes del periodo",
        String.valueOf(residentes),
        programas +
          (programas == 1 ? " programa" : " programas") +
          " con residentes",
        "azul"
      ),
      new Indicador(
        "con-rotacion",
        "Con rotación asignada",
        String.valueOf(conRotacion),
        detalleConRotacion(residentes, conRotacion, alcance),
        "teal"
      ),
      new Indicador(
        "rotaciones",
        "Rotaciones",
        String.valueOf(totales.rotaciones()),
        totales.asignaciones() + " asignaciones de residentes " + alcance,
        "naranja"
      ),
      new Indicador(
        "cruces",
        "Con cruce de fechas",
        String.valueOf(cruces.size()),
        "Residentes con dos rotaciones asignadas el mismo día",
        cruces.isEmpty() ? "verde" : "rojo"
      ),
      ocupacionMaxima(mapa, alcance)
    );
  }

  private static String detalleConRotacion(
    long residentes,
    long conRotacion,
    String alcance
  ) {
    if (residentes == 0) return "Sin residentes " + alcance;
    if (conRotacion == residentes) return "Todos con rotación asignada";
    long porcentaje = Math.round((conRotacion * 100.0) / residentes);
    return (
      porcentaje +
      "%; " +
      (residentes - conRotacion) +
      " solo en lista de espera"
    );
  }

  /** El servicio y el mes más presionados del mapa. */
  private static Indicador ocupacionMaxima(MapaOcupacion mapa, String alcance) {
    String servicio = null;
    Celda pico = null;
    for (FilaOcupacion fila : mapa.filas()) {
      for (Celda celda : fila.celdas()) {
        if (pico == null || celda.porcentaje() > pico.porcentaje()) {
          pico = celda;
          servicio = fila.servicio();
        }
      }
    }
    if (pico == null) return new Indicador(
      "ocupacion",
      "Ocupación máxima",
      "—",
      "Sin asignaciones " + alcance,
      "verde"
    );
    return new Indicador(
      "ocupacion",
      "Ocupación máxima",
      pico.porcentaje() + "%",
      servicio + " · " + mesLargo(pico.mes()),
      pico.porcentaje() > 100 ? "rojo" : "verde"
    );
  }

  private List<Alerta> alertas(
    List<ResidenteRevisar> cruces,
    List<ResidenteRevisar> enEspera,
    MapaOcupacion mapa,
    String alcance
  ) {
    long serviciosSobreCupo = mapa
      .filas()
      .stream()
      .filter(fila -> fila.diasExceso() > 0)
      .count();
    long serviciosSinCoordinador =
      dashboardRepository.serviciosSinCoordinador();

    List<Alerta> alertas = new ArrayList<>();
    if (!cruces.isEmpty()) alertas.add(
      new Alerta(
        "cruce",
        "critico",
        "Residentes con cruce de fechas",
        "Dos rotaciones asignadas comparten al menos un día",
        cruces.size()
      )
    );
    if (serviciosSobreCupo > 0) alertas.add(
      new Alerta(
        "sobre-cupo",
        "critico",
        "Servicios por encima del cupo",
        "Algún día superaron el cupo simultáneo",
        serviciosSobreCupo
      )
    );
    if (!enEspera.isEmpty()) alertas.add(
      new Alerta(
        "espera",
        "alerta",
        "Residentes en lista de espera",
        "Asignaciones que esperan cupo " + alcance,
        enEspera.size()
      )
    );
    if (serviciosSinCoordinador > 0) alertas.add(
      new Alerta(
        "sin-coordinador",
        "info",
        "Servicios sin coordinador",
        "Servicios activos sin coordinador activo",
        serviciosSinCoordinador
      )
    );
    return alertas;
  }

  /**
   * Barras de 1, 2… rotaciones; la última agrupa {@value #TOPE_CARGA} o más. «0 rotaciones»
   * solo aparece si hay residentes del periodo sin rotaciones en el servicio o asignatura.
   */
  private static List<Valor> carga(Map<Integer, Long> residentesPorRotaciones) {
    long[] conteo = new long[TOPE_CARGA + 1];
    residentesPorRotaciones.forEach(
      (rotaciones, residentes) ->
        conteo[Math.min(rotaciones, TOPE_CARGA)] += residentes
    );
    List<Valor> barras = new ArrayList<>();
    for (int i = conteo[0] > 0 ? 0 : 1; i <= TOPE_CARGA; i++) {
      String etiqueta =
        i == TOPE_CARGA
          ? TOPE_CARGA + " o más"
          : i + (i == 1 ? " rotación" : " rotaciones");
      barras.add(new Valor(etiqueta, conteo[i]));
    }
    return barras;
  }

  /** Todas las barras comparten la escala de la mayor. */
  private static List<Barra> barras(List<Conteo> conteos) {
    long maximo = conteos.stream().mapToLong(Conteo::valor).max().orElse(1);
    return conteos
      .stream()
      .map(conteo -> new Barra(conteo.etiqueta(), conteo.valor(), maximo))
      .toList();
  }

  /** Porcentaje de residentes por universidad, con un decimal; el resto va en «Otras». */
  private static List<Porcentaje> distribucion(List<Conteo> reparto) {
    double total = reparto.stream().mapToDouble(Conteo::valor).sum();
    boolean agrupar = reparto.size() > UNIVERSIDADES_VISIBLES + 1;
    List<Porcentaje> distribucion = new ArrayList<>();
    double otras = 0;
    for (int i = 0; i < reparto.size(); i++) {
      double porcentaje =
        total == 0 ? 0 : (reparto.get(i).valor() * 100) / total;
      if (agrupar && i >= UNIVERSIDADES_VISIBLES) otras += porcentaje;
      else distribucion.add(
        new Porcentaje(reparto.get(i).etiqueta(), unDecimal(porcentaje))
      );
    }
    if (otras > 0) distribucion.add(new Porcentaje("Otras", unDecimal(otras)));
    return distribucion;
  }

  private static List<Valor> valores(List<Conteo> conteos) {
    return conteos
      .stream()
      .map(conteo -> new Valor(conteo.etiqueta(), conteo.valor()))
      .toList();
  }

  private static double unDecimal(double valor) {
    return Math.round(valor * 10) / 10.0;
  }

  /** «2025-12» → «dic 2025». */
  private static String mesLargo(String mes) {
    return (
      MESES[Integer.parseInt(mes.substring(5)) - 1] + " " + mes.substring(0, 4)
    );
  }
}
