import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { FilaCargaInicial, ResultadoCargaInicial } from '../../../../../models/carga-inicial.model';
import { mensajeError } from '../../../../../services/api-error';
import { CargaInicialService } from '../../../../../services/carga-inicial.service';
import { cantidadConPalabra, segunCantidad } from '../emparejamiento';
import { CREAR_FALTANTES_EN_LA_VALIDACION, hayQueGuardar } from '../pasos-importacion';
import { ResultadoCarga } from '../resultado-carga/resultado-carga';
import { VistaPreviaCarga } from '../vista-previa-carga/vista-previa-carga';

/** Nombre legible de cada tabla en el resumen de registros nuevos del backend. */
const NOMBRE_DE_TABLA: Record<string, string> = {
  universidad: 'Universidades',
  programa: 'Programas',
  residente: 'Residentes',
  servicio: 'Servicios',
  asignatura: 'Asignaturas',
  rotacion: 'Rotaciones',
  rotacion_residente: 'Asignaciones de residentes',
};

/**
 * Paso 9: muestra qué se creará (resumen y vista previa fila por fila) y guarda la carga
 * con la huella de la última validación.
 * Si el backend detecta cambios desde la validación, el asistente obliga a validar de nuevo.
 */
@Component({
  selector: 'app-paso-confirmacion',
  imports: [ResultadoCarga, VistaPreviaCarga],
  templateUrl: './paso-confirmacion.html',
  styleUrl: './paso-confirmacion.css',
})
export class PasoConfirmacion {
  private readonly cargaInicialService = inject(CargaInicialService);

  readonly filas = input.required<FilaCargaInicial[]>();
  readonly filasExcluidas = input<FilaCargaInicial[]>([]);
  readonly resultadoValidacion = input.required<ResultadoCargaInicial>();
  readonly resultadoFinal = input<ResultadoCargaInicial | null>(null);
  readonly cargaConfirmada = output<ResultadoCargaInicial>();
  readonly confirmacionFallida = output<string>();
  readonly trabajandoCambiado = output<boolean>();
  readonly anterior = output<void>();
  readonly verRotaciones = output<void>();

  protected readonly guardando = signal(false);
  protected readonly hayQueGuardar = hayQueGuardar;
  protected readonly cantidadConPalabra = cantidadConPalabra;
  protected readonly segunCantidad = segunCantidad;

  /** Registros nuevos: los previstos antes de guardar o los creados después. */
  protected readonly registrosNuevos = computed(() => {
    const resultado = this.resultadoFinal() ?? this.resultadoValidacion();
    return Object.entries(resultado.nuevos).map(([tabla, cantidad]) => ({
      nombre: NOMBRE_DE_TABLA[tabla] ?? tabla,
      cantidad,
    }));
  });

  constructor() {
    effect(() => this.trabajandoCambiado.emit(this.guardando()));
  }

  protected async confirmarCarga() {
    if (this.guardando() || this.resultadoFinal() || !hayQueGuardar(this.resultadoValidacion()))
      return;
    this.guardando.set(true);
    try {
      const resultado = await firstValueFrom(
        this.cargaInicialService.confirmar(
          this.filas(),
          CREAR_FALTANTES_EN_LA_VALIDACION,
          this.resultadoValidacion().validacion,
        ),
      );
      this.cargaConfirmada.emit(resultado);
    } catch (e) {
      this.confirmacionFallida.emit(
        mensajeError(e) +
          ' Vuelve a validar antes de reintentar. Las relaciones existentes se reconocerán sin duplicarlas.',
      );
    } finally {
      this.guardando.set(false);
    }
  }
}
