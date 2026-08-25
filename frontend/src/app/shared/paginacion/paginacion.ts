import { Component, computed, input, output } from '@angular/core';

/** Corte entre dos tramos de páginas no contiguos. */
const CORTE = 'corte' as const;

type ItemPaginacion = number | typeof CORTE;

/** Páginas visibles a cada lado de la actual. */
const VENTANA = 1;

/**
 * A partir de este total se recorta con cortes. Por debajo caben todas las
 * páginas sin recortar y mostrarlas completas ahorra un clic.
 */
const MAXIMO_SIN_CORTES = 2 * VENTANA + 5;

@Component({
  selector: 'app-paginacion',
  imports: [],
  templateUrl: './paginacion.html',
  styleUrl: './paginacion.css',
})
export class Paginacion {
  readonly paginaActual = input.required<number>();
  readonly totalPaginas = input.required<number>();

  /** Página solicitada; el contenedor decide si la acepta. */
  readonly cambio = output<number>();

  /**
   * Primera y última siempre visibles, la actual con sus vecinas, y un corte
   * donde se salten páginas. Así el número de botones queda acotado por
   * mucho que crezca el listado cuando lleguen los datos del backend.
   */
  protected readonly items = computed<readonly ItemPaginacion[]>(() => {
    const total = this.totalPaginas();

    if (total <= MAXIMO_SIN_CORTES) {
      return Array.from({ length: total }, (_, indice) => indice + 1);
    }

    const actual = this.paginaActual();
    const desde = Math.max(2, actual - VENTANA);
    const hasta = Math.min(total - 1, actual + VENTANA);

    const items: ItemPaginacion[] = [1];

    if (desde > 2) {
      items.push(CORTE);
    }

    for (let pagina = desde; pagina <= hasta; pagina++) {
      items.push(pagina);
    }

    if (hasta < total - 1) {
      items.push(CORTE);
    }

    items.push(total);

    return items;
  });

  protected esPagina(item: ItemPaginacion): item is number {
    return item !== CORTE;
  }

  protected ir(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas() || pagina === this.paginaActual()) {
      return;
    }

    this.cambio.emit(pagina);
  }
}
