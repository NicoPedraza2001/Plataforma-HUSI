package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.response.DashboardResponse.Celda;
import co.edu.javeriana.husi.dto.response.DashboardResponse.FilaOcupacion;
import co.edu.javeriana.husi.dto.response.DashboardResponse.MapaOcupacion;
import co.edu.javeriana.husi.repository.DashboardRepository.Rango;
import co.edu.javeriana.husi.repository.DashboardRepository.Tramo;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Mapa de ocupación: pico mensual de residentes simultáneos por servicio frente a su cupo.
 * Recorre los días en Java para que la consulta funcione igual en PostgreSQL y en H2.
 */
final class OcupacionDashboard {

  /** Meses que cubre como máximo el mapa cuando no hay periodo. */
  static final int MESES_SIN_PERIODO = 12;

  private OcupacionDashboard() {}

  /**
   * Días que cubre el mapa: los 6 meses del periodo, o sin periodo los últimos
   * {@value #MESES_SIN_PERIODO} meses con asignaciones. Vacío si no hay asignaciones.
   */
  static Optional<Rango> ventana(String periodo, Optional<Rango> asignaciones) {
    if (periodo != null) {
      int anio = Integer.parseInt(periodo.substring(0, 4));
      LocalDate desde = LocalDate.of(anio, periodo.endsWith("1") ? 1 : 7, 1);
      return Optional.of(new Rango(desde, desde.plusMonths(6).minusDays(1)));
    }
    return asignaciones.map(rango -> {
      LocalDate hasta = YearMonth.from(rango.hasta()).atEndOfMonth();
      LocalDate primero = rango.desde().withDayOfMonth(1);
      LocalDate tope = hasta
        .withDayOfMonth(1)
        .minusMonths(MESES_SIN_PERIODO - 1);
      return new Rango(primero.isAfter(tope) ? primero : tope, hasta);
    });
  }

  /** Arma el mapa; primero los servicios más presionados, que son los que hay que mirar. */
  static MapaOcupacion calcular(Rango ventana, List<Tramo> tramos) {
    List<String> meses = meses(ventana);
    int totalDias =
      (int) ChronoUnit.DAYS.between(ventana.desde(), ventana.hasta()) + 1;
    // Por servicio: +1 el día que entra cada residente y -1 el día siguiente a su salida.
    Map<String, int[]> cambios = new TreeMap<>();
    Map<String, Integer> cupos = new HashMap<>();
    for (Tramo tramo : tramos) {
      cupos.put(tramo.servicio(), tramo.cupo());
      int[] dias = cambios.computeIfAbsent(
        tramo.servicio(),
        servicio -> new int[totalDias + 1]
      );
      dias[diasDesde(ventana, tramo.desde())]++;
      dias[diasDesde(ventana, tramo.hasta()) + 1]--;
    }
    List<FilaOcupacion> filas = new ArrayList<>();
    cambios.forEach((servicio, dias) ->
      filas.add(fila(servicio, cupos.get(servicio), dias, ventana, meses))
    );
    filas.sort(
      Comparator.comparingInt(FilaOcupacion::maximo)
        .reversed()
        .thenComparing(FilaOcupacion::servicio)
    );
    return new MapaOcupacion(meses, filas);
  }

  private static FilaOcupacion fila(
    String servicio,
    int cupo,
    int[] cambiosPorDia,
    Rango ventana,
    List<String> meses
  ) {
    Map<String, Integer> picos = new HashMap<>();
    long diasExceso = 0;
    int presentes = 0;
    for (int dia = 0; dia < cambiosPorDia.length - 1; dia++) {
      presentes += cambiosPorDia[dia];
      String mes = YearMonth.from(ventana.desde().plusDays(dia)).toString();
      picos.merge(mes, presentes, Math::max);
      if (presentes > cupo) diasExceso++;
    }
    int maximo = 0;
    List<Celda> celdas = new ArrayList<>();
    for (String mes : meses) {
      int pico = picos.getOrDefault(mes, 0);
      int porcentaje = cupo > 0 ? Math.round((pico * 100f) / cupo) : 0;
      maximo = Math.max(maximo, porcentaje);
      celdas.add(new Celda(mes, pico, porcentaje));
    }
    return new FilaOcupacion(servicio, cupo, diasExceso, celdas, maximo);
  }

  private static List<String> meses(Rango ventana) {
    List<String> meses = new ArrayList<>();
    for (
      YearMonth mes = YearMonth.from(ventana.desde());
      !mes.isAfter(YearMonth.from(ventana.hasta()));
      mes = mes.plusMonths(1)
    ) meses.add(mes.toString());
    return meses;
  }

  private static int diasDesde(Rango ventana, LocalDate dia) {
    return (int) ChronoUnit.DAYS.between(ventana.desde(), dia);
  }
}
