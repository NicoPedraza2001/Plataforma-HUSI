import { Component, computed, input } from '@angular/core';

/** Barra del gráfico horizontal; `maximo` fija la escala de la pista. */
export interface BarraDato {
  etiqueta: string;
  valor: number;
  maximo: number;
  /** Texto auxiliar bajo el valor, por ejemplo "92 / 120 cupos". */
  detalle?: string;
}

/** Nivel de severidad; solo se calcula cuando la entrada `severidad` está activa. */
type Nivel = 'base' | 'alerta' | 'critico';

@Component({
  selector: 'app-grafico-barras',
  imports: [],
  templateUrl: './grafico-barras.html',
  styleUrl: './grafico-barras.css',
})
export class GraficoBarras {
  readonly datos = input.required<readonly BarraDato[]>();
  /**
   * Qué se rotula al final de la barra: la cifra cruda o su porcentaje sobre
   * el máximo. En ocupación el porcentaje es lo que se compara entre servicios.
   */
  readonly formato = input<'valor' | 'porcentaje'>('valor');
  /**
   * Con severidad activa el color deja de ser identidad y pasa a indicar estado:
   * la ocupación alta se pinta como advertencia y la saturada como riesgo.
   */
  readonly severidad = input(false);
  protected readonly filas = computed(() =>
    this.datos().map((dato) => ({
      ...dato,
      porcentaje: dato.maximo > 0 ? Math.min(100, (dato.valor / dato.maximo) * 100) : 0,
      rotulo: this.rotulo(dato),
      nivel: this.nivel(dato),
    })),
  );

  private rotulo(dato: BarraDato): string {
    if (this.formato() === 'valor') {
      return `${dato.valor}`;
    }

    return dato.maximo > 0 ? `${Math.round((dato.valor / dato.maximo) * 100)}%` : '0%';
  }

  private nivel(dato: BarraDato): Nivel {
    if (!this.severidad() || dato.maximo <= 0) {
      return 'base';
    }

    const ocupacion = (dato.valor / dato.maximo) * 100;

    if (ocupacion >= 95) {
      return 'critico';
    }

    return ocupacion >= 85 ? 'alerta' : 'base';
  }
}
