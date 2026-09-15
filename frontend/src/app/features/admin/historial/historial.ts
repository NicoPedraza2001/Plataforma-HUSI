import { Component, computed, inject, signal } from '@angular/core';
import { HistorialService } from '../../../services/historial.service';
import { RegistroHistorial } from '../../../models/historial.model';
import { Consulta } from '../../../shared/consulta/consulta';
import { Pagina, Parametros } from '../../../models/pagina.model';
import { Catalogos } from '../../../models/catalogos.model';
import { CatalogoService } from '../../../services/catalogo.service';
import { Paginacion } from '../../../shared/paginacion/paginacion';
import { DatePipe } from '@angular/common';
@Component({
  selector: 'app-historial',
  imports: [Paginacion, DatePipe],
  templateUrl: './historial.html',
  styleUrl: './historial.css',
})
export class Historial {
  private readonly api = inject(HistorialService);
  private readonly catalogosApi = inject(CatalogoService);
  protected readonly consulta = new Consulta<Pagina<RegistroHistorial>>((params) =>
    this.api.listar(params),
  );
  protected readonly catalogos = new Consulta<Catalogos>(() => this.catalogosApi.listar());
  protected readonly termino = signal('');
  protected readonly paginaActual = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  private readonly paginaSolicitada = signal(1);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly desde = computed(() => (this.paginaActual() - 1) * 8);
  protected readonly registrosPagina = computed(() => this.consulta.datos()?.contenido ?? []);
  protected readonly cargando = computed(
    () => this.consulta.cargando() || this.catalogos.cargando(),
  );
  protected readonly error = computed(() => this.consulta.error() || this.catalogos.error());
  protected readonly moduloSeleccionado = signal('Todos');
  protected readonly accionSeleccionada = signal('Todas');
  protected readonly fechaInicial = signal('');
  protected readonly fechaFinal = signal('');
  protected readonly modulos = computed(() => [
    'Todos',
    ...(this.catalogos.datos()?.modulos ?? []),
  ]);
  protected readonly acciones = computed(() => [
    'Todas',
    ...(this.catalogos.datos()?.acciones ?? []),
  ]);
  protected claseBadge(accion: string) {
    return ['CREÓ', 'ACTIVÓ'].includes(accion)
      ? 'badge badge--activo'
      : accion === 'INACTIVÓ'
        ? 'badge badge--inactivo'
        : 'badge badge--info';
  }
  constructor() {
    this.cargar();
    this.catalogos.cargar();
  }
  protected reintentar() {
    this.cargar();
    this.catalogos.cargar();
  }
  protected cargar(espera = 0) {
    const params: Parametros = { q: this.termino(), pagina: this.paginaSolicitada(), tamano: 8 };
    if (this.moduloSeleccionado() !== 'Todos') params['modulo'] = this.moduloSeleccionado();
    if (this.accionSeleccionada() !== 'Todas') params['accion'] = this.accionSeleccionada();
    if (this.fechaInicial() !== '') params['desde'] = this.fechaInicial();
    if (this.fechaFinal() !== '') params['hasta'] = this.fechaFinal();
    this.consulta.cargar(params, espera);
  }
  protected buscar(valor: string) {
    this.termino.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar(300);
  }
  protected onModuloChange(valor: string) {
    this.moduloSeleccionado.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }
  protected onAccionChange(valor: string) {
    this.accionSeleccionada.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }
  protected onFechaInicialChange(valor: string) {
    this.fechaInicial.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }
  protected onFechaFinalChange(valor: string) {
    this.fechaFinal.set(valor);
    this.paginaSolicitada.set(1);
    this.cargar();
  }
  protected irAPagina(pagina: number) {
    if (pagina < 1 || pagina > this.totalPaginas() || this.cargando()) return;
    this.paginaSolicitada.set(pagina);
    this.cargar();
  }
}
