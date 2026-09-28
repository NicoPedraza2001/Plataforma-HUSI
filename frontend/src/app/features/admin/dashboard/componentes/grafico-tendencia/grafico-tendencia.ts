import { Component, computed, input, signal } from '@angular/core';

/** Punto de la serie temporal. */
export interface PuntoTendencia {
  etiqueta: string;
  valor: number;
}

/** Líneas de referencia horizontales, incluida la base. */
const DIVISIONES = 2;

@Component({
  selector: 'app-grafico-tendencia',
  imports: [],
  templateUrl: './grafico-tendencia.html',
  styleUrl: './grafico-tendencia.css',
})
export class GraficoTendencia {
  // Estado de la pantalla y datos del formulario
  readonly datos = input.required<readonly PuntoTendencia[]>();
  /** Punto sobre el que está el puntero o el foco del teclado. */
  protected readonly activo = signal<number | null>(null);
  /** Techo del eje redondeado hacia arriba para que las marcas queden limpias. */
  protected readonly techo = computed(() => {
    const maximo = Math.max(0, ...this.datos().map((punto) => punto.valor));
    const paso = maximo > 200 ? 50 : 10;

    return Math.max(paso, Math.ceil(maximo / paso) * paso);
  });
  /** Marcas del eje vertical, de arriba hacia abajo. */
  protected readonly marcas = computed(() =>
    Array.from({ length: DIVISIONES + 1 }, (_, indice) =>
      Math.round((this.techo() / DIVISIONES) * (DIVISIONES - indice)),
    ),
  );
  /** Coordenadas en porcentaje: el SVG se estira con el contenedor. */
  protected readonly puntos = computed(() => {
    const datos = this.datos();
    const techo = this.techo();

    return datos.map((punto, indice) => ({
      ...punto,
      x: datos.length > 1 ? (indice / (datos.length - 1)) * 100 : 50,
      y: 100 - (punto.valor / techo) * 100,
    }));
  });
  protected readonly linea = computed(() =>
    this.puntos()
      .map((punto, indice) => `${indice === 0 ? 'M' : 'L'}${punto.x} ${punto.y}`)
      .join(' '),
  );
  /** El área cierra contra la base para dar volumen a la serie. */
  protected readonly area = computed(() => `${this.linea()} L100 100 L0 100 Z`);
  protected readonly ultimo = computed(() => this.puntos().at(-1) ?? null);
  protected readonly detalle = computed(() => {
    const indice = this.activo();

    return indice === null ? null : (this.puntos()[indice] ?? null);
  });
}
