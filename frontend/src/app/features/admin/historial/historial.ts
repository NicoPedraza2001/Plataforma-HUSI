import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Catalogos } from '../../../models/catalogos.model';
import { RegistroHistorial } from '../../../models/historial.model';
import { Pagina, Parametros } from '../../../models/pagina.model';
import { CatalogoService } from '../../../services/catalogo.service';
import { HistorialService } from '../../../services/historial.service';
import { Consulta } from '../../../shared/consulta/consulta';
import { Paginacion } from '../../../shared/paginacion/paginacion';
import { claseBadgeAccion } from './comparar-datos';
import { DetalleMovimiento } from './detalle-movimiento/detalle-movimiento';

const MOVIMIENTOS_POR_PAGINA = 8;

@Component({
  selector: 'app-historial',
  imports: [Paginacion, DatePipe, DetalleMovimiento],
  templateUrl: './historial.html',
  styleUrl: './historial.css',
})
export class Historial {
  // Dependencias
  private readonly historialService = inject(HistorialService);
  private readonly catalogoService = inject(CatalogoService);

  // Estado de la pantalla y datos del formulario
  protected readonly consulta = new Consulta<Pagina<RegistroHistorial>>((params) =>
    this.historialService.listar(params),
  );
  protected readonly catalogos = new Consulta<Catalogos>(() => this.catalogoService.listar());
  protected readonly terminoBusqueda = signal('');
  protected readonly paginaActual = computed(
    () => this.consulta.datos()?.pagina ?? this.paginaSolicitada(),
  );
  private readonly paginaSolicitada = signal(1);
  protected readonly totalPaginas = computed(() => this.consulta.datos()?.totalPaginas ?? 1);
  protected readonly indicePrimeraFila = computed(
    () => (this.paginaActual() - 1) * MOVIMIENTOS_POR_PAGINA,
  );
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
  /** Movimiento cuyo detalle está abierto en la ventana emergente. */
  protected readonly idMovimientoAbierto = signal<number | null>(null);

  protected readonly claseBadgeAccion = claseBadgeAccion;

  constructor() {
    this.cargar();
    this.catalogos.cargar();
  }

  protected reintentar() {
    this.cargar();
    this.catalogos.cargar();
  }

  protected cargar(espera = 0) {
    const params: Parametros = {
      q: this.terminoBusqueda(),
      pagina: this.paginaSolicitada(),
      tamano: MOVIMIENTOS_POR_PAGINA,
    };
    if (this.moduloSeleccionado() !== 'Todos') params['modulo'] = this.moduloSeleccionado();
    if (this.accionSeleccionada() !== 'Todas') params['accion'] = this.accionSeleccionada();
    if (this.fechaInicial() !== '') params['desde'] = this.fechaInicial();
    if (this.fechaFinal() !== '') params['hasta'] = this.fechaFinal();
    this.consulta.cargar(params, espera);
  }

  protected buscar(valor: string) {
    this.terminoBusqueda.set(valor);
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

  protected abrirDetalle(idMovimiento: number) {
    this.idMovimientoAbierto.set(idMovimiento);
  }

  protected cerrarDetalle() {
    this.idMovimientoAbierto.set(null);
  }

  protected irAPagina(pagina: number) {
    if (pagina < 1 || pagina > this.totalPaginas() || this.cargando()) return;
    this.paginaSolicitada.set(pagina);
    this.cargar();
  }
}
