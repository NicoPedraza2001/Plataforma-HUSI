import { Component, computed, input, signal } from '@angular/core';
import { SituacionPrograma } from '../../../../../models/dashboard.model';

type Clave = 'conRotacion' | 'enEspera';

/**
 * Orden fijo de las categorías: el color sigue a la categoría, nunca a su tamaño, así que
 * un filtro que deja una en cero no repinta las demás.
 */
const CATEGORIAS: readonly { clave: Clave; etiqueta: string }[] = [
  { clave: 'conRotacion', etiqueta: 'Con rotación asignada' },
  { clave: 'enEspera', etiqueta: 'Solo en lista de espera' },
];

/** Segmento señalado por el puntero o el foco: fila y categoría. */
interface Activo {
  fila: number;
  clave: Clave;
}

@Component({
  selector: 'app-grafico-situacion',
  imports: [],
  templateUrl: './grafico-situacion.html',
  styleUrl: './grafico-situacion.css',
})
export class GraficoSituacion {
  readonly datos = input.required<readonly SituacionPrograma[]>();
  protected readonly categorias = CATEGORIAS;
  protected readonly vistaTabla = signal(false);
  protected readonly activo = signal<Activo | null>(null);

  protected readonly filas = computed(() =>
    this.datos().map((programa) => {
      const total = programa.conRotacion + programa.enEspera;
      return {
        ...programa,
        corta: abreviar(programa.etiqueta),
        total,
        segmentos: CATEGORIAS.map((categoria, indice) => ({
          ...categoria,
          indice,
          valor: programa[categoria.clave],
          porcentaje: total ? (programa[categoria.clave] / total) * 100 : 0,
        })).filter((segmento) => segmento.valor > 0),
      };
    }),
  );

  /** Totales de la leyenda: el valor se lee sin depender del color. */
  protected readonly totales = computed(() =>
    CATEGORIAS.map((categoria) => ({
      ...categoria,
      valor: this.datos().reduce((suma, programa) => suma + programa[categoria.clave], 0),
    })),
  );

  protected readonly detalle = computed(() => {
    const activo = this.activo();
    const fila = activo ? this.filas()[activo.fila] : null;
    const segmento = fila?.segmentos.find((s) => s.clave === activo?.clave);
    if (!fila || !segmento) return null;
    return {
      fila: activo!.fila,
      programa: fila.etiqueta,
      etiqueta: segmento.etiqueta,
      valor: segmento.valor,
      porcentaje: Math.round(segmento.porcentaje),
    };
  });

  protected resumen(fila: { etiqueta: string; total: number } & SituacionPrograma) {
    return `${fila.etiqueta}: ${fila.total} residentes. ${CATEGORIAS.map(
      (categoria) => `${categoria.etiqueta}: ${fila[categoria.clave]}`,
    ).join('. ')}.`;
  }
}

/** «Especialización en Medicina Interna» → «Medicina Interna»; el nombre completo va en el globo. */
function abreviar(programa: string) {
  return programa.replace(/^especializaci[oó]n en /i, '');
}
