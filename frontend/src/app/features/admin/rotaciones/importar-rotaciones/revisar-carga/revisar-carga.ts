import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { Paginacion } from '../../../../../shared/paginacion/paginacion';
import { CAMPOS_CARGA } from '../campos-carga';
import { normalizarBusqueda } from '../emparejamiento';

const FILAS_POR_PAGINA = 20;

/**
 * Tabla de filas de la carga: permite corregir una fila y cambiar su prioridad (el orden
 * decide quién ocupa primero los cupos). El número de fila siempre es el original del Excel.
 */
@Component({
  selector: 'app-revisar-carga',
  imports: [FormsModule, Paginacion],
  templateUrl: './revisar-carga.html',
  styleUrl: './revisar-carga.css',
})
export class RevisarCarga {
  readonly filas = input.required<FilaCargaInicial[]>();
  readonly bloqueado = input(false);
  readonly cambiar = output<FilaCargaInicial[]>();
  readonly edicion = output<boolean>();

  protected readonly campos = CAMPOS_CARGA;
  protected readonly paginaSolicitada = signal(1);
  protected readonly textoBusqueda = signal('');
  protected readonly filaEnEdicion = signal<FilaCargaInicial | null>(null);

  protected readonly filasFiltradas = computed(() => {
    const busqueda = normalizarBusqueda(this.textoBusqueda());
    return this.filas().filter((fila) =>
      normalizarBusqueda(
        [fila.fila, fila.residente, fila.documento, fila.servicio, fila.asignatura].join(' '),
      ).includes(busqueda),
    );
  });
  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filasFiltradas().length / FILAS_POR_PAGINA)),
  );
  protected readonly paginaActual = computed(() =>
    Math.min(this.paginaSolicitada(), this.totalPaginas()),
  );
  protected readonly filasVisibles = computed(() => {
    const inicio = (this.paginaActual() - 1) * FILAS_POR_PAGINA;
    return this.filasFiltradas().slice(inicio, inicio + FILAS_POR_PAGINA);
  });

  protected buscar(texto: string) {
    this.textoBusqueda.set(texto);
    this.paginaSolicitada.set(1);
  }

  /** Mueve la fila una posición: -1 sube su prioridad, +1 la baja. */
  protected moverFila(filaAMover: FilaCargaInicial, desplazamiento: number) {
    if (this.bloqueado()) return;
    const filasReordenadas = [...this.filas()];
    const posicionActual = filasReordenadas.findIndex((fila) => fila.fila === filaAMover.fila);
    const posicionNueva = posicionActual + desplazamiento;
    if (posicionNueva < 0 || posicionNueva >= filasReordenadas.length) return;
    [filasReordenadas[posicionActual], filasReordenadas[posicionNueva]] = [
      filasReordenadas[posicionNueva],
      filasReordenadas[posicionActual],
    ];
    this.cambiar.emit(filasReordenadas);
  }

  /** Abre el editor de una fila por su número en el Excel; lo usa «Corregir» del resultado. */
  editarFilaPorNumero(numeroFila: number) {
    const fila = this.filas().find((filaCarga) => filaCarga.fila === numeroFila);
    if (!fila) return;
    this.buscar(String(numeroFila));
    this.editarFila(fila);
  }

  protected editarFila(fila: FilaCargaInicial) {
    if (this.bloqueado()) return;
    this.filaEnEdicion.set({ ...fila });
    this.edicion.emit(true);
  }

  protected cancelarEdicion() {
    this.filaEnEdicion.set(null);
    this.edicion.emit(false);
  }

  protected guardarEdicion() {
    const filaCorregida = this.filaEnEdicion();
    if (!filaCorregida || this.bloqueado()) return;
    this.cambiar.emit(
      this.filas().map((fila) => (fila.fila === filaCorregida.fila ? { ...filaCorregida } : fila)),
    );
    this.cancelarEdicion();
  }
}
