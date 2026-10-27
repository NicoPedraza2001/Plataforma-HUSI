import { Component, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import {
  CAMPOS_CARGA,
  ConfiguracionColumnas,
  crearConfiguracionInicial,
  detectarColumnas,
  esPlantillaFinal,
  LibroExcel,
  MAXIMO_FILAS_CARGA,
  obtenerEncabezados,
  prepararCarga,
} from '../campos-carga';

/** Resultado del paso: la configuración elegida y las filas que produce. */
export interface ColumnasConfirmadas {
  configuracion: ConfiguracionColumnas;
  filas: FilaCargaInicial[];
}

/** Paso 2: relaciona cada campo de la carga con una columna del Excel o un valor común. */
@Component({
  selector: 'app-paso-columnas',
  imports: [FormsModule],
  templateUrl: './paso-columnas.html',
  styleUrl: './paso-columnas.css',
})
export class PasoColumnas implements OnInit {
  readonly libroExcel = input.required<LibroExcel>();
  /** Última configuración aplicada; al volver a este paso se recupera tal cual. */
  readonly configuracionGuardada = input<ConfiguracionColumnas | null>(null);
  readonly columnasConfirmadas = output<ColumnasConfirmadas>();
  readonly anterior = output<void>();

  protected readonly campos = CAMPOS_CARGA;
  protected readonly error = signal('');
  // Copia editable de la configuración, enlazada con ngModel.
  protected indiceHoja = 0;
  protected filaEncabezados = 1;
  protected columnaPorCampo: number[] = [];
  protected valorComunPorCampo: string[] = [];

  ngOnInit() {
    const configuracion =
      this.configuracionGuardada() ?? crearConfiguracionInicial(this.libroExcel().hojas);
    this.indiceHoja = configuracion.indiceHoja;
    this.filaEncabezados = configuracion.filaEncabezados;
    this.columnaPorCampo = [...configuracion.columnaPorCampo];
    this.valorComunPorCampo = [...configuracion.valorComunPorCampo];
  }

  protected encabezados(): string[] {
    return obtenerEncabezados(this.libroExcel().hojas, this.indiceHoja, this.filaEncabezados);
  }

  protected totalFilasDeLaHoja(): number {
    return this.libroExcel().hojas[this.indiceHoja]?.filas.length ?? 1;
  }

  protected esPlantillaInstitucional(): boolean {
    return esPlantillaFinal(this.encabezados());
  }

  protected cambiarHoja() {
    this.filaEncabezados = 1;
    this.detectarColumnasAutomaticamente();
  }

  protected detectarColumnasAutomaticamente() {
    this.columnaPorCampo = detectarColumnas(this.encabezados());
  }

  protected confirmarColumnas() {
    this.error.set('');
    const errorDeConfiguracion = this.validarConfiguracion();
    if (errorDeConfiguracion) {
      this.error.set(errorDeConfiguracion);
      return;
    }
    const celdasDeLaHoja = this.libroExcel().hojas[this.indiceHoja]?.filas ?? [];
    const filas = prepararCarga(
      celdasDeLaHoja,
      this.filaEncabezados,
      this.columnaPorCampo,
      this.valorComunPorCampo,
    );
    if (!filas.length || filas.length > MAXIMO_FILAS_CARGA) {
      this.error.set(`El archivo debe contener entre 1 y ${MAXIMO_FILAS_CARGA} filas de datos.`);
      return;
    }
    this.columnasConfirmadas.emit({
      configuracion: {
        indiceHoja: this.indiceHoja,
        filaEncabezados: this.filaEncabezados,
        columnaPorCampo: [...this.columnaPorCampo],
        valorComunPorCampo: [...this.valorComunPorCampo],
      },
      filas,
    });
  }

  /** Mensaje de error de la configuración actual, o cadena vacía si es válida. */
  private validarConfiguracion(): string {
    const columnasElegidas = this.columnaPorCampo.filter((columna) => columna >= 0);
    const filaEncabezadosValida =
      Number.isInteger(this.filaEncabezados) &&
      this.filaEncabezados >= 1 &&
      this.filaEncabezados <= this.totalFilasDeLaHoja();
    if (!filaEncabezadosValida) return 'La fila de encabezados no existe en la hoja elegida.';
    if (!columnasElegidas.length) return 'Relaciona al menos un campo con una columna del Excel.';
    if (new Set(columnasElegidas).size !== columnasElegidas.length)
      return 'Cada columna del Excel solo puede alimentar un campo.';
    if (columnasElegidas.some((columna) => columna >= this.encabezados().length))
      return 'Una de las columnas elegidas no existe en la fila de encabezados.';
    return '';
  }
}
