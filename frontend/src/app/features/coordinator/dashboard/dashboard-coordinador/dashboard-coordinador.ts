import { Component, computed, inject, signal } from '@angular/core';
import { ResidentesACargo } from '../../../../models/coordinador.model';
import { DashboardDatos } from '../../../../models/dashboard.model';
import { CoordinadorService } from '../../../../services/coordinador.service';
import { DashboardService } from '../../../../services/dashboard.service';
import { Consulta } from '../../../../shared/consulta/consulta';
import { BarraDato, GraficoBarras } from '../../../admin/dashboard/componentes/grafico-barras/grafico-barras';
import { GraficoTendencia } from '../../../admin/dashboard/componentes/grafico-tendencia/grafico-tendencia';
import { MapaOcupacion } from '../../../admin/dashboard/componentes/mapa-ocupacion/mapa-ocupacion';
import { TablaRevisar } from '../../../admin/dashboard/componentes/tabla-revisar/tabla-revisar';

const MAPA_VACIO = { meses: [], filas: [] };
/** Asignaturas visibles antes de pulsar «Ver todas»: algunos servicios tienen decenas. */
const ASIGNATURAS_INICIALES = 10;

/**
 * Dashboard del coordinador: los mismos widgets del panel de admin más relevantes para decidir
 * cupos, pero acotados siempre al servicio del coordinador en sesión (el backend lo filtra,
 * igual que en «Mis residentes»; nunca se elige un servicio desde aquí).
 */
@Component({
  selector: 'app-dashboard-coordinador',
  imports: [MapaOcupacion, GraficoTendencia, GraficoBarras, TablaRevisar],
  templateUrl: './dashboard-coordinador.html',
  styleUrl: './dashboard-coordinador.css',
})
export class DashboardCoordinador {
  // Dependencias
  private readonly dashboardService = inject(DashboardService);
  private readonly coordinadorService = inject(CoordinadorService);

  // Consultas al backend: el panel (ocupación, tendencia, a revisar) y los residentes (asignaturas)
  protected readonly panel = new Consulta<DashboardDatos>(() =>
    this.dashboardService.consultarCoordinador(),
  );
  protected readonly residentes = new Consulta<ResidentesACargo>(() =>
    this.coordinadorService.listarResidentes(),
  );

  protected readonly cargando = computed(() => this.panel.cargando() || this.residentes.cargando());
  protected readonly error = computed(() => this.panel.error() || this.residentes.error());

  protected readonly servicio = computed(() => this.residentes.datos()?.servicio ?? '');
  protected readonly mapaOcupacion = computed(() => this.panel.datos()?.mapaOcupacion ?? MAPA_VACIO);
  protected readonly tendenciaInicios = computed(() => this.panel.datos()?.tendenciaInicios ?? []);
  protected readonly totalInicios = computed(() =>
    this.tendenciaInicios().reduce((total, punto) => total + punto.valor, 0),
  );
  /** Cruces de fechas y lista de espera del servicio; ya vienen acotados desde el backend. */
  protected readonly revisar = computed(() => this.panel.datos()?.revisar ?? []);

  /** Cuenta rotaciones en curso y próximas del servicio, agrupadas por asignatura. */
  protected readonly rotacionesPorAsignatura = computed<BarraDato[]>(() => {
    const datos = this.residentes.datos();
    if (!datos) return [];
    const conteos = new Map<string, number>();
    for (const fila of [...datos.residentes, ...datos.proximos]) {
      conteos.set(fila.asignatura, (conteos.get(fila.asignatura) ?? 0) + 1);
    }
    const maximo = Math.max(1, ...conteos.values());
    return [...conteos.entries()]
      .map(([etiqueta, valor]) => ({ etiqueta, valor, maximo }))
      .sort((a, b) => b.valor - a.valor || a.etiqueta.localeCompare(b.etiqueta));
  });

  protected readonly asignaturasIniciales = ASIGNATURAS_INICIALES;
  protected readonly todasLasAsignaturas = signal(false);
  protected readonly asignaturasVisibles = computed(() =>
    this.todasLasAsignaturas()
      ? this.rotacionesPorAsignatura()
      : this.rotacionesPorAsignatura().slice(0, ASIGNATURAS_INICIALES),
  );

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.panel.cargar();
    this.residentes.cargar();
  }
}
