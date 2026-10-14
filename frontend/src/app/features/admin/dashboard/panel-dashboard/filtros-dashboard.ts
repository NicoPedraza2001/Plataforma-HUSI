import { computed, signal } from '@angular/core';
import { AsignaturaCatalogo } from '../../../../models/asignatura.model';
import { Catalogos } from '../../../../models/catalogos.model';
import { RelacionFiltros } from '../../../../models/dashboard.model';
import { Parametros } from '../../../../models/pagina.model';
import { CorteDashboard } from './reporte';

export type FiltroDashboard = 'periodo' | 'universidad' | 'programa' | 'servicio' | 'asignatura';

/** Datos que alimentan las opciones; cada función lee el signal de su consulta. */
export interface FuentesFiltros {
  catalogos: () => Catalogos | null;
  asignaturas: () => AsignaturaCatalogo[] | null;
  relaciones: () => RelacionFiltros[] | null;
}

const TODOS = 'Todos';
const TODAS = 'Todas';

/**
 * Filtros del dashboard: selección, opciones de cada lista y parámetros para el backend.
 * Programa, servicio y asignatura se acotan entre sí con las rotaciones registradas: con un
 * servicio elegido solo se ofrecen los programas cuyos residentes rotan allí, y viceversa.
 */
export class FiltrosDashboard {
  /** Periodo con el que abre el panel y al que vuelve «Limpiar filtros». */
  private readonly periodoInicial = signal(periodoActual());

  readonly periodo = signal(this.periodoInicial());
  readonly universidad = signal(TODAS);
  readonly programa = signal(TODOS);
  readonly servicio = signal(TODOS);
  /** Id de la asignatura como texto (valor del select) o «Todas». */
  readonly asignatura = signal(TODAS);

  /** Solo periodos con rotaciones: en cada uno se cuentan únicamente sus residentes. */
  readonly periodos = computed(() => {
    const periodos = this.fuentes.catalogos()?.periodos ?? [];
    return periodos.length ? periodos : [this.periodoInicial()];
  });

  readonly universidades = computed(() => [
    TODAS,
    ...(this.fuentes.catalogos()?.universidades ?? []),
  ]);

  readonly programas = computed(() => {
    const servicio = this.servicio();
    const asignatura = this.asignatura();
    const todos = this.fuentes.catalogos()?.programas ?? [];
    if (servicio === TODOS && asignatura === TODAS) return [TODOS, ...todos];
    const relacionados = this.relacionados(
      (relacion) =>
        (servicio === TODOS || relacion.servicio === servicio) &&
        (asignatura === TODAS || String(relacion.asignaturaId) === asignatura),
      (relacion) => relacion.programa,
    );
    return [TODOS, ...todos.filter((programa) => relacionados.has(programa))];
  });

  readonly servicios = computed(() => {
    const programa = this.programa();
    const todos = this.fuentes.catalogos()?.servicios ?? [];
    if (programa === TODOS) return [TODOS, ...todos];
    const relacionados = this.relacionados(
      (relacion) => relacion.programa === programa,
      (relacion) => relacion.servicio,
    );
    return [TODOS, ...todos.filter((servicio) => relacionados.has(servicio))];
  });

  /**
   * Asignaturas del servicio y del programa elegidos. Sin servicio, cada opción lleva el suyo
   * porque hay nombres repetidos (p. ej. «Cirugía oculoplástica» en tres servicios).
   */
  readonly asignaturas = computed(() => {
    const servicio = this.servicio();
    const programa = this.programa();
    const delPrograma =
      programa === TODOS
        ? null
        : this.relacionados(
            (relacion) => relacion.programa === programa,
            (relacion) => String(relacion.asignaturaId),
          );
    return (this.fuentes.asignaturas() ?? [])
      .filter((asignatura) => servicio === TODOS || asignatura.servicio === servicio)
      .filter((asignatura) => !delPrograma || delPrograma.has(String(asignatura.id)))
      .map((asignatura) => ({
        valor: String(asignatura.id),
        etiqueta:
          servicio === TODOS ? `${asignatura.nombre} · ${asignatura.servicio}` : asignatura.nombre,
      }));
  });

  /** Hay filtros distintos de los iniciales: habilita «Limpiar filtros». */
  readonly hayFiltros = computed(
    () =>
      this.periodo() !== this.periodoInicial() ||
      this.universidad() !== TODAS ||
      this.programa() !== TODOS ||
      this.servicio() !== TODOS ||
      this.asignatura() !== TODAS,
  );

  /** Selección vigente con textos legibles, para el reporte y los nombres de archivo. */
  readonly corte = computed<CorteDashboard>(() => {
    const asignaturaId = this.asignatura() === TODAS ? null : this.asignatura();
    return {
      periodo: this.periodo(),
      universidad: this.universidad(),
      programa: this.programa(),
      servicio: this.servicio(),
      asignatura:
        this.asignaturas().find((opcion) => opcion.valor === asignaturaId)?.etiqueta ?? TODAS,
      asignaturaId,
    };
  });

  /** Solo los filtros aplicados; el periodo siempre va. */
  readonly parametros = computed<Parametros>(() => {
    const corte = this.corte();
    const parametros: Parametros = { periodo: corte.periodo };
    if (corte.universidad !== TODAS) parametros['universidad'] = corte.universidad;
    if (corte.programa !== TODOS) parametros['programa'] = corte.programa;
    if (corte.servicio !== TODOS) parametros['servicio'] = corte.servicio;
    if (corte.asignaturaId) parametros['asignaturaId'] = corte.asignaturaId;
    return parametros;
  });

  constructor(private readonly fuentes: FuentesFiltros) {}

  /** Abre en el semestre en curso si tiene rotaciones; si no, en el más reciente que tenga. */
  iniciar(periodos: readonly string[]) {
    const inicial = elegirPeriodoInicial(periodos, periodoActual());
    this.periodoInicial.set(inicial);
    this.periodo.set(inicial);
  }

  elegir(filtro: FiltroDashboard, valor: string) {
    this[filtro].set(valor);
    this.ajustarDependientes();
  }

  limpiar() {
    this.periodo.set(this.periodoInicial());
    this.universidad.set(TODAS);
    this.programa.set(TODOS);
    this.servicio.set(TODOS);
    this.asignatura.set(TODAS);
  }

  /**
   * Un filtro que ya no está entre las opciones (p. ej. un servicio por el que no rota el
   * programa recién elegido) dejaría el corte vacío sin que se note por qué: vuelve a «Todos».
   */
  private ajustarDependientes() {
    if (!this.programas().includes(this.programa())) this.programa.set(TODOS);
    if (!this.servicios().includes(this.servicio())) this.servicio.set(TODOS);
    const asignatura = this.asignatura();
    const disponible = this.asignaturas().some((opcion) => opcion.valor === asignatura);
    if (asignatura !== TODAS && !disponible) this.asignatura.set(TODAS);
  }

  /** Valores de las relaciones que cumplen el criterio, para acotar una lista de opciones. */
  private relacionados(
    cumple: (relacion: RelacionFiltros) => boolean,
    valor: (relacion: RelacionFiltros) => string,
  ) {
    return new Set((this.fuentes.relaciones() ?? []).filter(cumple).map(valor));
  }
}

/** «Especialización en Geriatría» → «Geriatría»: todos los programas comparten el prefijo. */
export function nombreCorto(programa: string) {
  return programa.replace(/^especializaci[oó]n en /i, '');
}

/** Semestre de hoy en el formato de la plataforma: AAAA-1 (ene–jun) o AAAA-2 (jul–dic). */
export function periodoActual(hoy = new Date()) {
  return `${hoy.getFullYear()}-${hoy.getMonth() < 6 ? 1 : 2}`;
}

/**
 * El semestre en curso si tiene rotaciones; si no, el más reciente anterior a él (o el más
 * reciente de todos si solo hay periodos futuros). `periodos` llega ordenado de mayor a menor.
 */
export function elegirPeriodoInicial(periodos: readonly string[], actual: string) {
  if (periodos.includes(actual)) return actual;
  return periodos.find((periodo) => periodo <= actual) ?? periodos[0] ?? actual;
}
