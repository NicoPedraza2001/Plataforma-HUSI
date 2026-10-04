import { Component, computed, inject, signal } from '@angular/core';
import { Catalogos } from '../../../../models/catalogos.model';
import { DashboardDatos } from '../../../../models/dashboard.model';
import { Pagina, Parametros } from '../../../../models/pagina.model';
import { Rotacion } from '../../../../models/rotacion.model';
import { CatalogoService } from '../../../../services/catalogo.service';
import { DashboardService } from '../../../../services/dashboard.service';
import { RotacionService } from '../../../../services/rotacion.service';
import { Consulta } from '../../../../shared/consulta/consulta';
import { Paginacion } from '../../../../shared/paginacion/paginacion';
import { GraficoApilado } from '../componentes/grafico-apilado/grafico-apilado';
import { GraficoBarras } from '../componentes/grafico-barras/grafico-barras';
import { GraficoTendencia } from '../componentes/grafico-tendencia/grafico-tendencia';

@Component({
  selector: 'app-panel-dashboard',
  imports: [GraficoBarras, GraficoApilado, GraficoTendencia, Paginacion],
  templateUrl: './panel-dashboard.html',
  styleUrl: './panel-dashboard.css',
})
export class PanelDashboard {
  // Dependencias
  private readonly dashboardService = inject(DashboardService);
  private readonly rotacionService = inject(RotacionService);
  private readonly catalogoService = inject(CatalogoService);

  // Estado de la pantalla y datos del formulario
  protected readonly consulta = new Consulta<DashboardDatos>((params) =>
    this.dashboardService.consultar(params),
  );
  protected readonly rotaciones = new Consulta<Pagina<Rotacion>>((params) =>
    this.rotacionService.listar(params),
  );
  protected readonly catalogos = new Consulta<Catalogos>(() => this.catalogoService.listar());
  protected readonly periodoElegido = signal('Todos');
  protected readonly universidadElegida = signal('Todas');
  protected readonly servicioElegido = signal('Todos');
  protected readonly corte = signal({ periodo: 'Todos', universidad: 'Todas', servicio: 'Todos' });
  protected readonly periodos = computed(() => [
    'Todos',
    ...(this.catalogos.datos()?.periodos ?? []),
  ]);
  protected readonly universidades = computed(() => [
    'Todas',
    ...(this.catalogos.datos()?.universidades ?? []),
  ]);
  protected readonly servicios = computed(() => [
    'Todos',
    ...(this.catalogos.datos()?.servicios ?? []),
  ]);
  protected readonly cargando = computed(
    () => this.consulta.cargando() || this.rotaciones.cargando() || this.catalogos.cargando(),
  );
  protected readonly error = computed(
    () => this.consulta.error() || this.rotaciones.error() || this.catalogos.error(),
  );
  protected readonly indicadores = computed(() => this.consulta.datos()?.indicadores ?? []);
  protected readonly rotacionesPorServicio = computed(
    () => this.consulta.datos()?.rotacionesPorServicio ?? [],
  );
  protected readonly ocupacionPorServicio = computed(
    () => this.consulta.datos()?.ocupacionPorServicio ?? [],
  );
  protected readonly distribucionUniversidad = computed(
    () => this.consulta.datos()?.distribucionUniversidad ?? [],
  );
  protected readonly tendenciaInicios = computed(
    () => this.consulta.datos()?.tendenciaInicios ?? [],
  );
  protected readonly alertas = computed(() => this.consulta.datos()?.alertas ?? []);
  protected readonly totalInicios = computed(() =>
    this.tendenciaInicios().reduce((total, punto) => total + punto.valor, 0),
  );
  protected readonly rotacionesProximas = computed(() => this.rotaciones.datos()?.contenido ?? []);
  protected readonly paginaActual = computed(() => this.rotaciones.datos()?.pagina ?? 1);
  protected readonly totalPaginas = computed(() => this.rotaciones.datos()?.totalPaginas ?? 1);

  constructor() {
    this.catalogos.cargar();
    this.actualizar();
  }

  protected elegirPeriodo(valor: string) {
    this.periodoElegido.set(valor);
  }

  protected elegirUniversidad(valor: string) {
    this.universidadElegida.set(valor);
  }

  protected elegirServicio(valor: string) {
    this.servicioElegido.set(valor);
  }

  protected actualizar() {
    this.corte.set({
      periodo: this.periodoElegido(),
      universidad: this.universidadElegida(),
      servicio: this.servicioElegido(),
    });
    this.consulta.cargar(this.parametros());
    this.irAPagina(1);
  }

  protected reintentar() {
    this.catalogos.cargar();
    this.actualizar();
  }

  protected irAPagina(pagina: number) {
    this.rotaciones.cargar({ ...this.parametros(), pagina, tamano: 10, proximas: true });
  }

  protected etiquetaEstado(estado: string) {
    return estado;
  }

  private parametros(): Parametros {
    const parametros: Parametros = {};
    const corte = this.corte();
    if (corte.periodo !== 'Todos') parametros['periodo'] = corte.periodo;
    if (corte.universidad !== 'Todas') parametros['universidad'] = corte.universidad;
    if (corte.servicio !== 'Todos') parametros['servicio'] = corte.servicio;
    return parametros;
  }
}
