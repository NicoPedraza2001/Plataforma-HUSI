import { Component, computed, input } from '@angular/core';

/** Segmento de la barra apilada; el porcentaje es sobre el total de la barra. */
export interface SegmentoApilado {
  etiqueta: string;
  porcentaje: number;
}

/** Tope de segmentos con color propio; el resto debe agruparse en "Otras". */
const MAXIMO_SEGMENTOS = 4;

@Component({
  selector: 'app-grafico-apilado',
  imports: [],
  templateUrl: './grafico-apilado.html',
  styleUrl: './grafico-apilado.css',
})
export class GraficoApilado {
  readonly datos = input.required<readonly SegmentoApilado[]>();

  /**
   * El color se asigna por posición y nunca se recicla: si un filtro deja
   * fuera una entidad, las demás conservan su tono.
   */
  protected readonly segmentos = computed(() => this.datos().slice(0, MAXIMO_SEGMENTOS));

  /** Descripción textual de la barra para lectores de pantalla. */
  protected readonly resumen = computed(() =>
    this.segmentos()
      .map((segmento) => `${segmento.etiqueta} ${segmento.porcentaje}%`)
      .join(', '),
  );
}
