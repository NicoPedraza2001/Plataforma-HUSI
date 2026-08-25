import { Component, computed, signal } from '@angular/core';
import { BarraDato, GraficoBarras } from '../componentes/grafico-barras/grafico-barras';
import { GraficoApilado, SegmentoApilado } from '../componentes/grafico-apilado/grafico-apilado';
import {
  GraficoTendencia,
  PuntoTendencia,
} from '../componentes/grafico-tendencia/grafico-tendencia';
import {
  ALERTAS,
  ETIQUETAS_ESTADO,
  EstadoRotacion,
  FACTOR_PERIODO,
  FORMA_INICIOS,
  FiltroServicio,
  FiltroUniversidad,
  MESES_PERIODO,
  OPCIONES_SERVICIO,
  OPCIONES_UNIVERSIDAD,
  PERIODOS,
  Periodo,
  ROTACIONES_PROXIMAS,
  SERVICIOS_BASE,
  TODAS,
  TODOS,
  UNIVERSIDADES,
} from '../datos-demo';

/** Tarjeta de indicador de la fila superior. */
interface Indicador {
  id: string;
  etiqueta: string;
  valor: string;
  detalle: string;
  /** Variación frente al periodo anterior; se omite cuando no hay con qué comparar. */
  delta?: string;
  tono: 'teal' | 'naranja' | 'azul' | 'verde';
}

/** Corte que se está mirando: los tres filtros aplicados a la vez. */
interface Corte {
  periodo: Periodo;
  universidad: FiltroUniversidad;
  servicio: FiltroServicio;
}

/** Cifras agregadas de un corte. */
interface Totales {
  activas: number;
  proximas: number;
  residentes: number;
  /** Rotaciones que ocupan cupo, sin recortar por universidad. */
  usados: number;
  cupos: number;
}

const CORTE_INICIAL: Corte = { periodo: PERIODOS[0], universidad: TODAS, servicio: TODOS };

/**
 * Reparte `total` entre `pesos` conservando la suma exacta: la parte entera a
 * cada uno y el sobrante a los de mayor decimal. Sin esto la serie mensual
 * redondeada no cuadra con el KPI de rotaciones que está justo encima.
 */
function repartir(total: number, pesos: readonly number[]): number[] {
  const suma = pesos.reduce((acumulado, peso) => acumulado + peso, 0);

  if (suma <= 0) {
    return pesos.map(() => 0);
  }

  const exactos = pesos.map((peso) => (total * peso) / suma);
  const reparto = exactos.map((valor) => Math.floor(valor));

  let sobrante = total - reparto.reduce((acumulado, valor) => acumulado + valor, 0);

  const porDecimal = exactos
    .map((valor, indice) => ({ indice, decimal: valor - Math.floor(valor) }))
    .sort((a, b) => b.decimal - a.decimal);

  for (const { indice } of porDecimal) {
    if (sobrante <= 0) {
      break;
    }

    reparto[indice]++;
    sobrante--;
  }

  return reparto;
}

@Component({
  selector: 'app-panel-dashboard',
  imports: [GraficoBarras, GraficoApilado, GraficoTendencia],
  templateUrl: './panel-dashboard.html',
  styleUrl: './panel-dashboard.css',
})
export class PanelDashboard {
  protected readonly periodos = PERIODOS;
  protected readonly universidades = OPCIONES_UNIVERSIDAD;
  protected readonly servicios = OPCIONES_SERVICIO;
  protected readonly alertas = ALERTAS;

  /**
   * Selección en curso. Se separa del corte aplicado porque "Actualizar" es lo
   * que dispara la consulta: cuando exista el backend, mover un select no debe
   * lanzar una petición por cada cambio.
   */
  protected readonly periodoElegido = signal<Periodo>(CORTE_INICIAL.periodo);
  protected readonly universidadElegida = signal<FiltroUniversidad>(CORTE_INICIAL.universidad);
  protected readonly servicioElegido = signal<FiltroServicio>(CORTE_INICIAL.servicio);

  private readonly corteAplicado = signal<Corte>(CORTE_INICIAL);

  /** Corte con el que están calculados los paneles que se ven ahora mismo. */
  protected readonly corte = this.corteAplicado.asReadonly();

  protected readonly hayCambios = computed(() => {
    const aplicado = this.corteAplicado();

    return (
      aplicado.periodo !== this.periodoElegido() ||
      aplicado.universidad !== this.universidadElegida() ||
      aplicado.servicio !== this.servicioElegido()
    );
  });

  /** Servicios que entran en el corte; con el filtro en "Todos" son los cinco. */
  private readonly serviciosDelCorte = computed(() => {
    const elegido = this.corteAplicado().servicio;

    return SERVICIOS_BASE.filter((base) => elegido === TODOS || base.servicio === elegido);
  });

  /** Rotaciones activas por servicio ya recortadas por periodo y universidad. */
  private readonly demandaPorServicio = computed(() => {
    const { periodo, universidad } = this.corteAplicado();
    const factor = FACTOR_PERIODO[periodo];

    return this.serviciosDelCorte().map((base) => ({
      servicio: base.servicio,
      activas: Math.round(base.activas * factor * cuota(base.reparto, universidad)),
    }));
  });

  private readonly totalesActuales = computed(() => this.totales(this.corteAplicado().periodo));

  /** Periodo con el que se compara; el más antiguo de la lista no tiene anterior. */
  private readonly periodoAnterior = computed(() =>
    PERIODOS.at(PERIODOS.indexOf(this.corteAplicado().periodo) + 1),
  );

  private readonly totalesAnteriores = computed(() => {
    const anterior = this.periodoAnterior();

    return anterior ? this.totales(anterior) : null;
  });

  protected readonly indicadores = computed<readonly Indicador[]>(() => {
    const actual = this.totalesActuales();
    const previo = this.totalesAnteriores();
    const ocupacion = porcentaje(actual.usados, actual.cupos);

    return [
      {
        id: 'activas',
        etiqueta: 'Rotaciones activas',
        valor: `${actual.activas}`,
        detalle: 'En curso',
        delta: this.delta(actual.activas, previo?.activas),
        tono: 'teal',
      },
      {
        id: 'proximas',
        etiqueta: 'Próximas a iniciar',
        valor: `${actual.proximas}`,
        detalle: 'Próximos 30 días',
        delta: this.delta(actual.proximas, previo?.proximas),
        tono: 'naranja',
      },
      {
        id: 'residentes',
        etiqueta: 'Residentes activos',
        valor: `${actual.residentes}`,
        detalle: 'Con rotación vigente',
        delta: this.delta(actual.residentes, previo?.residentes),
        tono: 'azul',
      },
      {
        id: 'ocupacion',
        etiqueta: 'Ocupación de cupos',
        valor: `${ocupacion}%`,
        detalle: 'Sobre la capacidad del servicio',
        delta: previo
          ? this.delta(ocupacion, porcentaje(previo.usados, previo.cupos), ' pts')
          : undefined,
        tono: 'verde',
      },
    ];
  });

  protected readonly rotacionesPorServicio = computed<readonly BarraDato[]>(() => {
    const filas = this.demandaPorServicio();
    const maximo = Math.max(1, ...filas.map((fila) => fila.activas));

    return filas
      .map((fila) => ({ etiqueta: fila.servicio, valor: fila.activas, maximo }))
      .sort((a, b) => b.valor - a.valor);
  });

  /**
   * Ordenado de mayor a menor saturación: lo primero que se lee es el cuello
   * de botella. No se recorta por universidad porque el cupo lo pone el
   * servicio y compararlo contra una sola universidad no mide saturación.
   */
  protected readonly ocupacionPorServicio = computed<readonly BarraDato[]>(() => {
    const factor = FACTOR_PERIODO[this.corteAplicado().periodo];

    return this.serviciosDelCorte()
      .map((base) => {
        const usados = Math.round(base.activas * factor);

        return {
          etiqueta: base.servicio,
          valor: usados,
          maximo: base.cupos,
          detalle: `${usados} / ${base.cupos} cupos`,
        };
      })
      .sort((a, b) => b.valor / b.maximo - a.valor / a.maximo);
  });

  /**
   * Reparto completo entre universidades. Es lo que el panel existe para
   * mostrar, así que el filtro de universidad no lo recorta: dejarlo en un
   * único segmento al 100% no diría nada.
   */
  protected readonly distribucionUniversidad = computed<readonly SegmentoApilado[]>(() => {
    const filas = this.serviciosDelCorte();
    const total = filas.reduce((acumulado, base) => acumulado + base.activas, 0);

    if (total === 0) {
      return [];
    }

    const pesos = UNIVERSIDADES.map((universidad) =>
      filas.reduce((acumulado, base) => acumulado + base.activas * base.reparto[universidad], 0),
    );

    return repartir(100, pesos).map((puntos, indice) => ({
      etiqueta: UNIVERSIDADES[indice],
      porcentaje: puntos,
    }));
  });

  protected readonly tendenciaInicios = computed<readonly PuntoTendencia[]>(() => {
    const { periodo } = this.corteAplicado();
    const meses = MESES_PERIODO[periodo];

    return repartir(this.totalesActuales().activas, FORMA_INICIOS[periodo]).map((valor, indice) => ({
      etiqueta: meses[indice],
      valor,
    }));
  });

  protected readonly rotacionesProximas = computed(() => {
    const { periodo, universidad, servicio } = this.corteAplicado();

    return ROTACIONES_PROXIMAS.filter(
      (rotacion) =>
        rotacion.periodo === periodo &&
        (universidad === TODAS || rotacion.universidad === universidad) &&
        (servicio === TODOS || rotacion.servicio === servicio),
    );
  });

  /** Total de la serie mostrada, usado como subtítulo del panel de tendencia. */
  protected readonly totalInicios = computed(() =>
    this.tendenciaInicios().reduce((suma, punto) => suma + punto.valor, 0),
  );

  protected etiquetaEstado(estado: EstadoRotacion): string {
    return ETIQUETAS_ESTADO[estado];
  }

  // Los valores de los selects salen de nuestras propias listas, así que la
  // conversión desde el string del DOM es segura.
  protected elegirPeriodo(valor: string): void {
    this.periodoElegido.set(valor as Periodo);
  }

  protected elegirUniversidad(valor: string): void {
    this.universidadElegida.set(valor as FiltroUniversidad);
  }

  protected elegirServicio(valor: string): void {
    this.servicioElegido.set(valor as FiltroServicio);
  }

  protected actualizar(): void {
    // TODO: aquí irá la petición de métricas al backend con este mismo corte.
    this.corteAplicado.set({
      periodo: this.periodoElegido(),
      universidad: this.universidadElegida(),
      servicio: this.servicioElegido(),
    });
  }

  private totales(periodo: Periodo): Totales {
    const { universidad } = this.corteAplicado();
    const factor = FACTOR_PERIODO[periodo];

    return this.serviciosDelCorte().reduce<Totales>(
      (acumulado, base) => {
        const parte = cuota(base.reparto, universidad);

        return {
          activas: acumulado.activas + Math.round(base.activas * factor * parte),
          proximas: acumulado.proximas + Math.round(base.proximas * factor * parte),
          residentes: acumulado.residentes + Math.round(base.residentes * factor * parte),
          usados: acumulado.usados + Math.round(base.activas * factor),
          cupos: acumulado.cupos + base.cupos,
        };
      },
      { activas: 0, proximas: 0, residentes: 0, usados: 0, cupos: 0 },
    );
  }

  private delta(actual: number, previo: number | undefined, sufijo = ''): string | undefined {
    const anterior = this.periodoAnterior();

    if (previo === undefined || anterior === undefined) {
      return undefined;
    }

    const diferencia = actual - previo;

    // Sin variación no hay nada que contar: "+0" solo ocupa sitio.
    if (diferencia === 0) {
      return undefined;
    }

    return `${diferencia > 0 ? '+' : '-'}${Math.abs(diferencia)}${sufijo} vs ${anterior}`;
  }
}

/** Fracción de las rotaciones de un servicio que aporta la universidad filtrada. */
function cuota(reparto: Record<string, number>, universidad: FiltroUniversidad): number {
  return universidad === TODAS ? 1 : reparto[universidad] / 100;
}

function porcentaje(parte: number, total: number): number {
  return total > 0 ? Math.round((parte / total) * 100) : 0;
}
