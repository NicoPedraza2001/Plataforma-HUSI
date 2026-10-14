import { Component, computed, input, linkedSignal, model, signal } from '@angular/core';
import { MotivoRevision, ResidenteRevisar } from '../../../../../models/dashboard.model';
import { descargarLibro } from '../../../../../shared/excel/excel';
import { Paginacion } from '../../../../../shared/paginacion/paginacion';
import { MOTIVOS } from '../../panel-dashboard/reporte';

const POR_PAGINA = 10;
/** Un residente puede tener muchos cruces: se ven los primeros y el resto al desplegar. */
const LINEAS_VISIBLES = 2;

/**
 * Residentes a revisar (cruces de fechas y lista de espera) con filtro por motivo, paginación
 * y exportación a Excel. Llegan completos del backend, así que todo se resuelve en el navegador.
 */
@Component({
  selector: 'app-tabla-revisar',
  imports: [Paginacion],
  templateUrl: './tabla-revisar.html',
  styleUrl: './tabla-revisar.css',
})
export class TablaRevisar {
  readonly residentes = input.required<readonly ResidenteRevisar[]>();
  /** Periodo del corte, para nombrar el archivo exportado. */
  readonly periodo = input.required<string>();
  /** Motivo elegido; el panel lo fija al pulsar un aviso. */
  readonly motivo = model<MotivoRevision | 'todos'>('todos');

  protected readonly motivos = MOTIVOS;
  protected readonly lineasVisibles = LINEAS_VISIBLES;
  protected readonly expandidos = signal<ReadonlySet<string>>(new Set());
  protected readonly exportando = signal(false);
  /** Vuelve a la primera página cuando cambian los datos o el motivo. */
  protected readonly pagina = linkedSignal({
    source: () => [this.residentes(), this.motivo()],
    computation: () => 1,
  });

  protected readonly chips = computed(() => {
    const residentes = this.residentes();
    return [
      { motivo: 'todos' as const, etiqueta: 'Todos', cantidad: residentes.length },
      ...(Object.keys(MOTIVOS) as MotivoRevision[]).map((motivo) => ({
        motivo,
        etiqueta: MOTIVOS[motivo].etiqueta,
        cantidad: residentes.filter((residente) => residente.motivo === motivo).length,
      })),
    ].filter((chip) => chip.motivo === 'todos' || chip.cantidad > 0);
  });
  protected readonly filtrados = computed(() => {
    const motivo = this.motivo();
    return motivo === 'todos'
      ? this.residentes()
      : this.residentes().filter((residente) => residente.motivo === motivo);
  });
  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filtrados().length / POR_PAGINA)),
  );
  protected readonly filas = computed(() => {
    const inicio = (this.pagina() - 1) * POR_PAGINA;
    // El detalle trae un cruce por línea; se separa aquí para mostrarlos como lista.
    return this.filtrados()
      .slice(inicio, inicio + POR_PAGINA)
      .map((residente) => ({
        ...residente,
        clave: residente.motivo + residente.residenteId,
        lineas: residente.detalle.split('\n'),
      }));
  });

  protected alternarDetalle(clave: string) {
    const expandidos = new Set(this.expandidos());
    if (!expandidos.delete(clave)) expandidos.add(clave);
    this.expandidos.set(expandidos);
  }

  /** Exporta lo que se ve con el motivo elegido, sin paginar. */
  protected async exportar() {
    this.exportando.set(true);
    try {
      await descargarLibro(
        `residentes-a-revisar-${this.periodo()}.xlsx`,
        'Residentes a revisar',
        ['Residente', 'Programa', 'Motivo', 'Detalle'],
        this.filtrados().map((residente) => [
          residente.residente,
          residente.programa,
          MOTIVOS[residente.motivo].etiqueta,
          residente.detalle,
        ]),
      );
    } finally {
      this.exportando.set(false);
    }
  }
}
