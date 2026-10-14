import { Component, computed, input, signal } from '@angular/core';
import { MapaOcupacion as DatosMapa } from '../../../../../models/dashboard.model';

/** Filas visibles antes de pulsar «Ver todos»: los servicios más presionados van primero. */
const FILAS_INICIALES = 10;
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

/**
 * Escala secuencial de un solo tono (más ocupación, más oscuro). Por encima del cupo deja
 * de ser magnitud y pasa a estado: se pinta como crítico y lleva el porcentaje rotulado.
 */
type Nivel = 0 | 1 | 2 | 3 | 4 | 'exceso';
const ESCALA: readonly { nivel: Nivel; etiqueta: string }[] = [
  { nivel: 1, etiqueta: '1–25%' },
  { nivel: 2, etiqueta: '26–50%' },
  { nivel: 3, etiqueta: '51–75%' },
  { nivel: 4, etiqueta: '76–100%' },
  { nivel: 'exceso', etiqueta: 'Sobre el cupo' },
];

@Component({
  selector: 'app-mapa-ocupacion',
  imports: [],
  templateUrl: './mapa-ocupacion.html',
  styleUrl: './mapa-ocupacion.css',
})
export class MapaOcupacion {
  readonly datos = input.required<DatosMapa>();
  protected readonly escala = ESCALA;
  protected readonly filasIniciales = FILAS_INICIALES;
  protected readonly vistaTabla = signal(false);
  protected readonly todos = signal(false);
  protected readonly activo = signal<{ fila: number; columna: number } | null>(null);

  protected readonly meses = computed(() =>
    this.datos().meses.map((mes) => ({ clave: mes, corta: mesCorto(mes) })),
  );
  protected readonly filas = computed(() =>
    this.datos().filas.map((fila) => ({
      ...fila,
      celdas: fila.celdas.map((celda) => ({ ...celda, nivel: nivel(celda.porcentaje) })),
    })),
  );
  protected readonly visibles = computed(() =>
    this.todos() ? this.filas() : this.filas().slice(0, FILAS_INICIALES),
  );
  protected readonly ocultas = computed(() => Math.max(0, this.filas().length - FILAS_INICIALES));
  protected readonly columnas = computed(
    () => `minmax(120px, 170px) repeat(${this.meses().length}, minmax(30px, 1fr))`,
  );

  protected readonly detalle = computed(() => {
    const activo = this.activo();
    if (!activo) return null;
    const fila = this.visibles()[activo.fila];
    const celda = fila?.celdas[activo.columna];
    if (!fila || !celda) return null;
    return {
      ...activo,
      servicio: fila.servicio,
      mes: mesLargo(celda.mes),
      pico: celda.pico,
      cupo: fila.cupo,
      porcentaje: celda.porcentaje,
    };
  });

  protected etiquetaFila(fila: {
    servicio: string;
    cupo: number;
    maximo: number;
    diasExceso: number;
  }) {
    const exceso = fila.diasExceso
      ? ` Superó el cupo ${fila.diasExceso} ${fila.diasExceso === 1 ? 'día' : 'días'}.`
      : '';
    return `${fila.servicio}, cupo ${fila.cupo}: pico de ${fila.maximo}%.${exceso}`;
  }
}

function nivel(porcentaje: number): Nivel {
  if (porcentaje > 100) return 'exceso';
  if (porcentaje <= 0) return 0;
  return Math.min(4, Math.ceil(porcentaje / 25)) as Nivel;
}

/** «2025-08» → «ago 25». */
function mesCorto(mes: string) {
  const [anio, numero] = mes.split('-');
  return `${MESES[Number(numero) - 1]} ${anio.slice(2)}`;
}

/** «2025-08» → «ago 2025». */
function mesLargo(mes: string) {
  const [anio, numero] = mes.split('-');
  return `${MESES[Number(numero) - 1]} ${anio}`;
}
