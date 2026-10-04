import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Paginacion } from '../../../../../shared/paginacion/paginacion';
import {
  CLASE_BADGE_ESTADO,
  describirFilas,
  ETIQUETA_ESTADO,
  GrupoEmparejamiento,
  OPCIONES_FILTRO_ESTADO,
} from '../emparejamiento';
import { FiltroDeTabla } from '../filtro-de-tabla';

/** Elección de un registro existente para reemplazar el valor del Excel. */
export interface AsociacionElegida {
  grupo: GrupoEmparejamiento;
  registroId: number;
}

/**
 * Tabla de valores del Excel frente a la base de datos, reutilizada por los pasos
 * Universidades y Programas. Solo presenta y emite intenciones; el paso decide qué
 * hacer con cada una (llamar al backend, abrir el formulario, reescribir filas).
 */
@Component({
  selector: 'app-tabla-emparejamiento',
  imports: [FormsModule, Paginacion],
  templateUrl: './tabla-emparejamiento.html',
  styleUrl: './tabla-emparejamiento.css',
})
export class TablaEmparejamiento {
  readonly grupos = input.required<GrupoEmparejamiento[]>();
  /** Nombre del dato en singular, para los textos: «universidad», «programa». */
  readonly nombreEntidad = input.required<string>();
  /** Encabezado de la columna de contexto: «País», «Universidad». */
  readonly tituloContexto = input.required<string>();
  readonly bloqueado = input(false);

  readonly crear = output<GrupoEmparejamiento>();
  readonly asociar = output<AsociacionElegida>();
  readonly activar = output<GrupoEmparejamiento>();
  readonly usarDatosDelSistema = output<GrupoEmparejamiento>();
  readonly excluir = output<GrupoEmparejamiento>();

  protected readonly opcionesFiltro = OPCIONES_FILTRO_ESTADO;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO;
  protected readonly claseBadgeEstado = CLASE_BADGE_ESTADO;
  protected readonly describirFilas = describirFilas;
  protected readonly filtro = new FiltroDeTabla(
    () => this.grupos(),
    (grupo) => `${grupo.valorEnExcel} ${grupo.contexto} ${grupo.valoresOriginales.join(' ')}`,
  );

  protected elegirRegistroExistente(grupo: GrupoEmparejamiento, evento: Event) {
    const selector = evento.target as HTMLSelectElement;
    const registroId = Number(selector.value);
    selector.value = '';
    if (registroId) this.asociar.emit({ grupo, registroId });
  }
}
