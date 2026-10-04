import { computed, signal } from '@angular/core';
import {
  cumpleFiltroEstado,
  EstadoEmparejamiento,
  FiltroEstado,
  normalizarBusqueda,
  ordenarPendientesPrimero,
} from './emparejamiento';

/**
 * Filtro por estado, búsqueda de texto y paginación local para las tablas del asistente.
 * Lo pendiente se muestra primero. Se instancia como campo del componente.
 */
export class FiltroDeTabla<T extends { estado: EstadoEmparejamiento }> {
  readonly filtroEstado = signal<FiltroEstado>('todos');
  readonly textoBusqueda = signal('');
  readonly paginaSolicitada = signal(1);

  readonly elementosFiltrados = computed(() => {
    const busqueda = normalizarBusqueda(this.textoBusqueda());
    const filtrados = this.obtenerElementos().filter(
      (elemento) =>
        cumpleFiltroEstado(elemento, this.filtroEstado()) &&
        normalizarBusqueda(this.obtenerTextoBuscable(elemento)).includes(busqueda),
    );
    return ordenarPendientesPrimero(filtrados);
  });
  readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.elementosFiltrados().length / this.elementosPorPagina)),
  );
  readonly paginaActual = computed(() => Math.min(this.paginaSolicitada(), this.totalPaginas()));
  readonly elementosVisibles = computed(() => {
    const inicio = (this.paginaActual() - 1) * this.elementosPorPagina;
    return this.elementosFiltrados().slice(inicio, inicio + this.elementosPorPagina);
  });

  constructor(
    private readonly obtenerElementos: () => T[],
    private readonly obtenerTextoBuscable: (elemento: T) => string,
    private readonly elementosPorPagina = 15,
  ) {}

  cambiarFiltro(filtro: FiltroEstado) {
    this.filtroEstado.set(filtro);
    this.paginaSolicitada.set(1);
  }

  cambiarBusqueda(texto: string) {
    this.textoBusqueda.set(texto);
    this.paginaSolicitada.set(1);
  }
}
