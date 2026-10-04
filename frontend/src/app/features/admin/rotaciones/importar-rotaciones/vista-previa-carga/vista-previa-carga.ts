import { Component, computed, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FilaCargaInicial, ResultadoCargaInicial } from '../../../../../models/carga-inicial.model';
import { Paginacion } from '../../../../../shared/paginacion/paginacion';
import { normalizarBusqueda, segunCantidad } from '../emparejamiento';

/** Fila que se guardará al confirmar, con lo que creará además de la asignación. */
export interface FilaAGuardar {
  /** Posición en la carga: decide quién ocupa primero los cupos. */
  prioridad: number;
  fila: FilaCargaInicial;
  registrosNuevos: string[];
  /** No alcanzó cupo: se guardará en lista de espera. */
  enEspera: boolean;
}

/** Nombre legible de las tablas que el backend anuncia en «Se crearán: …». */
const REGISTRO_NUEVO_POR_TABLA: Record<string, string> = {
  universidad: 'Universidad',
  programa: 'Programa',
  residente: 'Residente',
  servicio: 'Servicio',
  asignatura: 'Asignatura',
  rotacion: 'Rotación',
};

const FILAS_POR_PAGINA = 20;

/**
 * Lee del mensaje de validación del backend («… Se crearán: asignatura, rotacion,
 * rotacion_residente.») qué registros nuevos produce la fila, además de la asignación.
 */
export function leerRegistrosNuevos(mensajeDeValidacion: string): string[] {
  const [, listaDeTablas = ''] = mensajeDeValidacion.split('Se crearán:');
  return listaDeTablas
    .replace(/\.\s*$/, '')
    .split(',')
    .map((tabla) => REGISTRO_NUEVO_POR_TABLA[tabla.trim()])
    .filter((nombre): nombre is string => !!nombre);
}

/**
 * Vista previa del paso Confirmar: lista, en orden de prioridad, cada fila que se guardará
 * (con cupo o en lista de espera) con todos sus datos para revisar exactamente lo que se
 * va a guardar.
 */
@Component({
  selector: 'app-vista-previa-carga',
  imports: [FormsModule, Paginacion],
  templateUrl: './vista-previa-carga.html',
  styleUrl: './vista-previa-carga.css',
})
export class VistaPreviaCarga {
  readonly filas = input.required<FilaCargaInicial[]>();
  readonly resultadoValidacion = input.required<ResultadoCargaInicial>();

  protected readonly segunCantidad = segunCantidad;
  protected readonly textoBusqueda = signal('');
  protected readonly paginaSolicitada = signal(1);

  protected readonly filasAGuardar = computed<FilaAGuardar[]>(() => {
    const detallePorFila = new Map(
      this.resultadoValidacion().filas.map((detalle) => [detalle.fila, detalle]),
    );
    return this.filas()
      .map((fila, posicion) => ({
        fila,
        prioridad: posicion + 1,
        detalle: detallePorFila.get(fila.fila),
      }))
      .filter(({ detalle }) => detalle?.estado === 'VALIDO' || detalle?.estado === 'ESPERA')
      .map(({ fila, prioridad, detalle }) => ({
        fila,
        prioridad,
        registrosNuevos: leerRegistrosNuevos(detalle!.mensaje),
        enEspera: detalle!.estado === 'ESPERA',
      }));
  });
  protected readonly totalFilasQueNoSeImportan = computed(
    () => this.filas().length - this.filasAGuardar().length,
  );
  protected readonly filasFiltradas = computed(() => {
    const busqueda = normalizarBusqueda(this.textoBusqueda());
    return this.filasAGuardar().filter(({ fila }) =>
      normalizarBusqueda(
        [
          fila.fila,
          fila.residente,
          fila.documento,
          fila.universidad,
          fila.programa,
          fila.servicio,
          fila.asignatura,
          fila.inicio,
        ].join(' '),
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

  protected creaServicio(filaAGuardar: FilaAGuardar): boolean {
    return filaAGuardar.registrosNuevos.includes(REGISTRO_NUEVO_POR_TABLA['servicio']);
  }
}
