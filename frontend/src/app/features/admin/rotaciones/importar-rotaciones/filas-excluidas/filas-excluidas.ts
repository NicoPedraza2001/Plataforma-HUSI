import { Component, input, output } from '@angular/core';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { IdPasoImportacion, PASOS_IMPORTACION } from '../pasos-importacion';

/** Fila que el administrador quitó de la importación, tal como estaba al excluirla. */
export interface FilaExcluida {
  fila: FilaCargaInicial;
  /** Paso en el que se excluyó: al restaurarla se vuelve a revisar desde ahí. */
  paso: IdPasoImportacion;
}

/**
 * Lista plegable de las filas excluidas en los pasos de emparejamiento, con la opción de
 * devolverlas a la importación. Se muestra encima de cada paso mientras haya alguna.
 */
@Component({
  selector: 'app-filas-excluidas',
  templateUrl: './filas-excluidas.html',
  styleUrl: './filas-excluidas.css',
})
export class FilasExcluidas {
  readonly filas = input.required<FilaExcluida[]>();
  /** Números de fila del Excel que se devuelven a la importación. */
  readonly restaurar = output<number[]>();

  protected tituloDelPaso(idPaso: IdPasoImportacion): string {
    return PASOS_IMPORTACION.find((paso) => paso.id === idPaso)?.titulo ?? idPaso;
  }

  protected restaurarTodas() {
    this.restaurar.emit(this.filas().map(({ fila }) => fila.fila));
  }
}
