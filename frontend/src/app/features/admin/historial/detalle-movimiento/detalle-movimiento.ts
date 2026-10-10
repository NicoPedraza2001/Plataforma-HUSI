import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DetalleMovimiento as Movimiento } from '../../../../models/historial.model';
import { mensajeError } from '../../../../services/api-error';
import { HistorialService } from '../../../../services/historial.service';
import { FocoModal } from '../../../../shared/foco-modal/foco-modal';
import { claseBadgeAccion, compararDatos } from '../comparar-datos';

/** Backend: HistorialAuditoriaServiceImpl.CLAVE_ESTADO_IMPORTACION e IMPORTACION_CANCELADA. */
const CLAVE_ESTADO_IMPORTACION = 'Estado de la importación';
const IMPORTACION_CANCELADA = 'Cancelada';

/**
 * Ventana con todo lo que se sabe de un movimiento del historial: quién lo hizo, cuándo, sobre
 * qué registro y cómo estaba el registro antes y después. Una importación muestra su resumen.
 */
@Component({
  selector: 'app-detalle-movimiento',
  imports: [DatePipe, FocoModal],
  templateUrl: './detalle-movimiento.html',
  styleUrl: './detalle-movimiento.css',
})
export class DetalleMovimiento {
  // Dependencias
  private readonly historialService = inject(HistorialService);

  readonly idMovimiento = input.required<number>();
  readonly cerrar = output<void>();

  protected readonly movimiento = signal<Movimiento | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal('');
  protected readonly soloModificados = signal(false);

  protected readonly campos = computed(() => {
    const movimiento = this.movimiento();
    if (!movimiento) return [];
    return compararDatos(movimiento.datosAnteriores, movimiento.datosNuevos);
  });
  protected readonly camposVisibles = computed(() =>
    this.soloModificados() ? this.campos().filter((campo) => campo.fueModificado) : this.campos(),
  );
  protected readonly cantidadModificados = computed(
    () => this.campos().filter((campo) => campo.fueModificado).length,
  );
  /** Actualizar, activar, trasladar…: hay un antes y un después que comparar. */
  protected readonly esComparacion = computed(() => {
    const movimiento = this.movimiento();
    return !!movimiento?.datosAnteriores && !!movimiento?.datosNuevos;
  });
  /** Se abandonó el asistente antes de confirmar; lo creado en sus pasos se conserva. */
  protected readonly importacionCancelada = computed(() => {
    const movimiento = this.movimiento();
    return (
      !!movimiento?.esImportacion &&
      movimiento.datosNuevos?.[CLAVE_ESTADO_IMPORTACION] === IMPORTACION_CANCELADA
    );
  });
  protected readonly tituloDatos = computed(() => {
    const movimiento = this.movimiento();
    if (!movimiento) return '';
    if (movimiento.esImportacion) return 'Resumen de la importación';
    if (this.esComparacion()) return 'Cambios realizados';
    return movimiento.datosNuevos ? 'Datos registrados' : 'Datos eliminados';
  });

  protected readonly claseBadgeAccion = claseBadgeAccion;

  constructor() {
    effect(() => this.cargar(this.idMovimiento()));
  }

  protected async cargar(id: number) {
    this.cargando.set(true);
    this.error.set('');
    try {
      this.movimiento.set(await firstValueFrom(this.historialService.consultar(id)));
    } catch (e) {
      this.error.set(mensajeError(e));
    } finally {
      this.cargando.set(false);
    }
  }

  protected alternarSoloModificados() {
    this.soloModificados.update((activo) => !activo);
  }
}
