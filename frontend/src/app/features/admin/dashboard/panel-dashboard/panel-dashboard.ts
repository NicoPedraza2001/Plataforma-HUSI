import { NgTemplateOutlet } from '@angular/common';
import {
  Component,
  computed,
  effect,
  inject,
  linkedSignal,
  signal,
  untracked,
} from '@angular/core';
import { AsignaturaCatalogo } from '../../../../models/asignatura.model';
import { Catalogos } from '../../../../models/catalogos.model';
import {
  DashboardDatos,
  MotivoRevision,
  RelacionFiltros,
} from '../../../../models/dashboard.model';
import { Pagina } from '../../../../models/pagina.model';
import { Rotacion } from '../../../../models/rotacion.model';
import { AsignaturaService } from '../../../../services/asignatura.service';
import { CatalogoService } from '../../../../services/catalogo.service';
import { DashboardService } from '../../../../services/dashboard.service';
import { RotacionService } from '../../../../services/rotacion.service';
import { Consulta } from '../../../../shared/consulta/consulta';
import { descargarHojas } from '../../../../shared/excel/excel';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { GraficoApilado } from '../componentes/grafico-apilado/grafico-apilado';
import { BarraDato, GraficoBarras } from '../componentes/grafico-barras/grafico-barras';
import { GraficoSituacion } from '../componentes/grafico-situacion/grafico-situacion';
import { GraficoTendencia } from '../componentes/grafico-tendencia/grafico-tendencia';
import { MapaOcupacion } from '../componentes/mapa-ocupacion/mapa-ocupacion';
import { TablaRevisar } from '../componentes/tabla-revisar/tabla-revisar';
import { FiltroDashboard, FiltrosDashboard, nombreCorto } from './filtros-dashboard';
import { armarReporte, MOTIVOS, nombreReporte } from './reporte';

const SERVICIOS_INICIALES = 10;

@Component({
  selector: 'app-panel-dashboard',
  imports: [
    NgTemplateOutlet,
    GraficoBarras,
    GraficoApilado,
    GraficoTendencia,
    GraficoSituacion,
    MapaOcupacion,
    TablaRevisar,
    Paginacion,
  ],
  templateUrl: './panel-dashboard.html',
  styleUrl: './panel-dashboard.css',
})
export class PanelDashboard {
  // Dependencias
  private readonly dashboardService = inject(DashboardService);
  private readonly rotacionService = inject(RotacionService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly asignaturaService = inject(AsignaturaService);

  // Consultas al backend
  protected readonly consulta = new Consulta<DashboardDatos>((params) =>
    this.dashboardService.consultar(params),
  );
  protected readonly rotaciones = new Consulta<Pagina<Rotacion>>((params) =>
    this.rotacionService.listar(params),
  );
  protected readonly catalogos = new Consulta<Catalogos>(() => this.catalogoService.listar());
  protected readonly catalogoAsignaturas = new Consulta<AsignaturaCatalogo[]>(() =>
    this.asignaturaService.catalogo(),
  );
  protected readonly relaciones = new Consulta<RelacionFiltros[]>(() =>
    this.dashboardService.relaciones(),
  );

  // Filtros: opciones encadenadas, selección y parámetros
  protected readonly filtros = new FiltrosDashboard({
    catalogos: this.catalogos.datos,
    asignaturas: this.catalogoAsignaturas.datos,
    relaciones: this.relaciones.datos,
  });
  protected readonly nombreCorto = nombreCorto;

  /**
   * Últimos datos recibidos. Al cambiar un filtro la consulta se vacía mientras llega la nueva
   * respuesta; conservar la anterior (atenuada) evita que el panel parpadee en cada cambio.
   */
  protected readonly datos = linkedSignal<DashboardDatos | null, DashboardDatos | null>({
    source: this.consulta.datos,
    computation: (nuevos, previo) => nuevos ?? previo?.value ?? null,
  });
  private readonly proximas = linkedSignal<Pagina<Rotacion> | null, Pagina<Rotacion> | null>({
    source: this.rotaciones.datos,
    computation: (nuevas, previo) => nuevas ?? previo?.value ?? null,
  });
  protected readonly cargando = computed(
    () =>
      this.consulta.cargando() ||
      this.rotaciones.cargando() ||
      this.catalogos.cargando() ||
      this.catalogoAsignaturas.cargando() ||
      this.relaciones.cargando(),
  );
  protected readonly error = computed(
    () =>
      this.consulta.error() ||
      this.rotaciones.error() ||
      this.catalogos.error() ||
      this.catalogoAsignaturas.error() ||
      this.relaciones.error(),
  );

  // Secciones del panel
  protected readonly indicadores = computed(() => this.datos()?.indicadores ?? []);
  protected readonly situacionPorPrograma = computed(
    () => this.datos()?.situacionPorPrograma ?? [],
  );
  protected readonly alertas = computed(() => this.datos()?.alertas ?? []);
  protected readonly revisar = computed(() => this.datos()?.revisar ?? []);
  /** Motivo de la tabla de residentes a revisar; lo fijan los avisos al pulsarse. */
  protected readonly motivoRevisar = signal<MotivoRevision | 'todos'>('todos');
  protected readonly mapaOcupacion = computed(
    () => this.datos()?.mapaOcupacion ?? { meses: [], filas: [] },
  );
  protected readonly tendenciaInicios = computed(() => this.datos()?.tendenciaInicios ?? []);
  protected readonly totalInicios = computed(() =>
    this.tendenciaInicios().reduce((total, punto) => total + punto.valor, 0),
  );
  protected readonly cargaResidentes = computed<BarraDato[]>(() => {
    const carga = this.datos()?.cargaResidentes ?? [];
    const maximo = Math.max(1, ...carga.map((barra) => barra.valor));
    return carga.map((barra) => ({ ...barra, maximo }));
  });
  /** De mayor a menor: lo primero que se busca es dónde se concentran las rotaciones. */
  protected readonly rotacionesPorServicio = computed(() =>
    [...(this.datos()?.rotacionesPorServicio ?? [])].sort(
      (a, b) => b.valor - a.valor || a.etiqueta.localeCompare(b.etiqueta),
    ),
  );
  protected readonly serviciosIniciales = SERVICIOS_INICIALES;
  protected readonly todosLosServicios = signal(false);
  protected readonly serviciosVisibles = computed(() =>
    this.todosLosServicios()
      ? this.rotacionesPorServicio()
      : this.rotacionesPorServicio().slice(0, SERVICIOS_INICIALES),
  );
  protected readonly distribucionUniversidad = computed(
    () => this.datos()?.distribucionUniversidad ?? [],
  );
  /** Con una sola universidad el reparto es siempre 100% y no aporta nada. */
  protected readonly mostrarReparto = computed(() => this.distribucionUniversidad().length > 1);
  protected readonly rotacionesProximas = computed(() => this.proximas()?.contenido ?? []);
  protected readonly paginaActual = computed(() => this.proximas()?.pagina ?? 1);
  protected readonly totalPaginas = computed(() => this.proximas()?.totalPaginas ?? 1);
  protected readonly generandoReporte = signal(false);

  constructor() {
    this.catalogos.cargar();
    this.catalogoAsignaturas.cargar();
    this.relaciones.cargar();
    // La primera consulta espera al catálogo para abrir en un periodo que tenga datos.
    let iniciado = false;
    effect(() => {
      const periodos = this.catalogos.datos()?.periodos;
      if (!periodos || iniciado) return;
      iniciado = true;
      untracked(() => {
        this.filtros.iniciar(periodos);
        this.actualizar();
      });
    });
  }

  /** Cada filtro se aplica en cuanto cambia: no hace falta pulsar ningún botón. */
  protected cambiarFiltro(filtro: FiltroDashboard, valor: string) {
    this.filtros.elegir(filtro, valor);
    this.actualizar();
  }

  protected limpiarFiltros() {
    this.filtros.limpiar();
    this.actualizar();
  }

  protected reintentar() {
    this.catalogos.cargar();
    this.catalogoAsignaturas.cargar();
    this.relaciones.cargar();
    this.actualizar();
  }

  /** Las próximas rotaciones son la agenda desde hoy: no dependen del periodo elegido. */
  protected irAPagina(pagina: number) {
    const filtros = { ...this.filtros.parametros() };
    delete filtros['periodo'];
    this.rotaciones.cargar({ ...filtros, pagina, tamano: 10, proximas: true });
  }

  /** Las alertas llevan a donde se resuelven: la tabla filtrada o el mapa de ocupación. */
  protected verAlerta(id: string) {
    if (id in MOTIVOS) {
      this.motivoRevisar.set(id as MotivoRevision);
      desplazarA('revisar');
    } else if (id === 'sobre-cupo') {
      desplazarA('ocupacion');
    }
  }

  protected alertaNavegable(id: string) {
    return id in MOTIVOS || id === 'sobre-cupo';
  }

  /** Descarga en Excel todo lo que muestra el panel para el corte vigente. */
  protected async descargarReporte() {
    const datos = this.datos();
    if (!datos) return;
    this.generandoReporte.set(true);
    try {
      const corte = this.filtros.corte();
      await descargarHojas(nombreReporte(corte), armarReporte(datos, corte));
    } finally {
      this.generandoReporte.set(false);
    }
  }

  private actualizar() {
    this.consulta.cargar(this.filtros.parametros());
    this.motivoRevisar.set('todos');
    this.irAPagina(1);
  }
}

function desplazarA(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}
