import { Component, effect, inject, input, output, signal, viewChild } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { FilaCargaInicial, ResultadoCargaInicial } from '../../../../../models/carga-inicial.model';
import { mensajeError } from '../../../../../services/api-error';
import { CargaInicialService } from '../../../../../services/carga-inicial.service';
import {
  CREAR_FALTANTES_EN_LA_VALIDACION,
  hayQueGuardar,
  IdPasoImportacion,
} from '../pasos-importacion';
import { CorreccionSolicitada, ResultadoCarga } from '../resultado-carga/resultado-carga';
import { RevisarCarga } from '../revisar-carga/revisar-carga';

/**
 * Paso 8: con universidades, programas, servicios, asignaturas y residentes ya resueltos,
 * revisa fechas y prioridad de cada fila y valida todo contra la base de datos sin guardar.
 * Los errores llevan al paso donde se corrigen.
 */
@Component({
  selector: 'app-paso-rotaciones',
  imports: [RevisarCarga, ResultadoCarga],
  templateUrl: './paso-rotaciones.html',
  styleUrl: './paso-rotaciones.css',
})
export class PasoRotaciones {
  private readonly cargaInicialService = inject(CargaInicialService);
  private readonly revisarCarga = viewChild(RevisarCarga);

  readonly filas = input.required<FilaCargaInicial[]>();
  readonly resultadoValidacion = input<ResultadoCargaInicial | null>(null);
  readonly filasCambiadas = output<FilaCargaInicial[]>();
  readonly validacionCompletada = output<ResultadoCargaInicial>();
  readonly trabajandoCambiado = output<boolean>();
  readonly irAPaso = output<IdPasoImportacion>();
  readonly anterior = output<void>();
  readonly siguiente = output<void>();

  protected readonly hayQueGuardar = hayQueGuardar;
  protected readonly validando = signal(false);
  protected readonly editandoFila = signal(false);
  protected readonly error = signal('');

  constructor() {
    effect(() => this.trabajandoCambiado.emit(this.validando() || this.editandoFila()));
  }

  protected async validarConLaBaseDeDatos() {
    if (this.validando() || this.editandoFila() || !this.filas().length) return;
    this.validando.set(true);
    this.error.set('');
    try {
      const resultado = await firstValueFrom(
        this.cargaInicialService.validar(this.filas(), CREAR_FALTANTES_EN_LA_VALIDACION),
      );
      this.validacionCompletada.emit(resultado);
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.validando.set(false);
    }
  }

  protected corregirFila({ numeroFila, paso }: CorreccionSolicitada) {
    if (paso === 'rotaciones') this.revisarCarga()?.editarFilaPorNumero(numeroFila);
    else this.irAPaso.emit(paso);
  }
}
