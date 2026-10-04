import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FilaCargaInicial, ResultadoCargaInicial } from '../../../../../models/carga-inicial.model';
import { descargarLibro } from '../../../../../shared/excel/excel';
import { Paginacion } from '../../../../../shared/paginacion/paginacion';
import { CAMPOS_CARGA } from '../campos-carga';
import { buscarPasoParaCorregir, IdPasoImportacion, PASOS_IMPORTACION } from '../pasos-importacion';

type DetalleDeFila = ResultadoCargaInicial['filas'][number];

/** Fila con error que el administrador quiere corregir en el paso correspondiente. */
export interface CorreccionSolicitada {
  numeroFila: number;
  paso: IdPasoImportacion;
}

const ETIQUETA_RESULTADO: Partial<Record<string, string>> = {
  VALIDO: 'Válida',
  CREADO: 'Creada',
  ESPERA: 'En espera',
  ERROR: 'Error',
  OMITIDO: 'Omitida',
};

const FILAS_POR_PAGINA = 20;

/**
 * Resumen y detalle por fila de una validación o de la carga final del backend. Cada error
 * ofrece ir al paso donde se corrige; también descarga el reporte con los datos.
 */
@Component({
  selector: 'app-resultado-carga',
  imports: [FormsModule, Paginacion],
  templateUrl: './resultado-carga.html',
  styleUrl: './resultado-carga.css',
})
export class ResultadoCarga {
  readonly resultado = input.required<ResultadoCargaInicial>();
  readonly filas = input.required<FilaCargaInicial[]>();
  /** Filas excluidas a mano en el asistente: tampoco se crearon. */
  readonly filasExcluidas = input<FilaCargaInicial[]>([]);
  /** Verdadero cuando el resultado es de la carga ya guardada. */
  readonly terminado = input(false);
  readonly corregir = output<CorreccionSolicitada>();

  protected readonly etiquetaResultado = ETIQUETA_RESULTADO;
  protected readonly filtroResultado = signal('TODOS');
  protected readonly paginaSolicitada = signal(1);
  protected readonly errorDescarga = signal('');

  protected readonly detallesFiltrados = computed(() =>
    this.resultado().filas.filter(
      (detalle) => this.filtroResultado() === 'TODOS' || detalle.estado === this.filtroResultado(),
    ),
  );
  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.detallesFiltrados().length / FILAS_POR_PAGINA)),
  );
  protected readonly paginaActual = computed(() =>
    Math.min(this.paginaSolicitada(), this.totalPaginas()),
  );
  protected readonly detallesVisibles = computed(() => {
    const inicio = (this.paginaActual() - 1) * FILAS_POR_PAGINA;
    return this.detallesFiltrados().slice(inicio, inicio + FILAS_POR_PAGINA);
  });

  /**
   * Tras guardar: filas que el backend no creó más las excluidas en el asistente. Las que
   * quedaron en lista de espera sí se crearon.
   */
  protected readonly noCreados = computed(() => {
    const detallePorFila = new Map(
      this.resultado().filas.map((detalle) => [detalle.fila, detalle]),
    );
    const rechazadas = this.filas()
      .filter((fila) => !['CREADO', 'ESPERA'].includes(detallePorFila.get(fila.fila)?.estado ?? ''))
      .map((fila) => {
        const detalle = detallePorFila.get(fila.fila);
        return {
          fila,
          motivo: detalle
            ? (ETIQUETA_RESULTADO[detalle.estado] ?? detalle.estado)
            : 'Sin resultado',
          detalle: detalle?.mensaje ?? 'El servidor no devolvió resultado para esta fila.',
        };
      });
    const excluidas = this.filasExcluidas().map((fila) => ({
      fila,
      motivo: 'Excluida',
      detalle: 'Se excluyó manualmente durante la revisión del asistente.',
    }));
    return [...rechazadas, ...excluidas].sort((a, b) => a.fila.fila - b.fila.fila);
  });

  protected cambiarFiltro(filtro: string) {
    this.filtroResultado.set(filtro);
    this.paginaSolicitada.set(1);
  }

  /** Paso donde se corrige la fila, solo para errores que el asistente puede resolver. */
  protected pasoParaCorregir(detalle: DetalleDeFila): IdPasoImportacion | null {
    if (this.terminado() || detalle.estado !== 'ERROR') return null;
    return buscarPasoParaCorregir(detalle.mensaje);
  }

  protected tituloDelPaso(paso: IdPasoImportacion): string {
    return PASOS_IMPORTACION.find((pasoImportacion) => pasoImportacion.id === paso)?.titulo ?? '';
  }

  protected async descargarReporte() {
    this.errorDescarga.set('');
    const detallePorFila = new Map(
      this.resultado().filas.map((detalle) => [detalle.fila, detalle]),
    );
    const filasDelReporte = this.filas().map((fila) => {
      const detalle = detallePorFila.get(fila.fila);
      return [
        ...CAMPOS_CARGA.map((campo) => fila[campo.clave]),
        fila.fila,
        detalle ? (ETIQUETA_RESULTADO[detalle.estado] ?? detalle.estado) : '',
        detalle?.mensaje ?? '',
      ];
    });
    try {
      await descargarLibro(
        'resultado-carga-inicial.xlsx',
        'Rotaciones',
        [...CAMPOS_CARGA.map((campo) => campo.nombre), 'Fila original', 'Resultado', 'Detalle'],
        filasDelReporte,
      );
    } catch {
      this.errorDescarga.set('No se pudo descargar el reporte.');
    }
  }

  /**
   * Excel con los registros que no se crearon. Usa los mismos encabezados de la carga para
   * poder corregirlo e importarlo de nuevo; «Motivo» y «Detalle» son informativas.
   */
  protected async descargarNoCreados() {
    this.errorDescarga.set('');
    try {
      await descargarLibro(
        'registros-no-creados.xlsx',
        'No creados',
        [...CAMPOS_CARGA.map((campo) => campo.nombre), 'Fila original', 'Motivo', 'Detalle'],
        this.noCreados().map(({ fila, motivo, detalle }) => [
          ...CAMPOS_CARGA.map((campo) => fila[campo.clave]),
          fila.fila,
          motivo,
          detalle,
        ]),
      );
    } catch {
      this.errorDescarga.set('No se pudo descargar el archivo de registros no creados.');
    }
  }
}
