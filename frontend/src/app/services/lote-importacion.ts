import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { HistorialService } from './historial.service';

export interface Lote {
  id: string;
  nombreArchivo: string;
}

/**
 * Importación de Excel en curso. El asistente de carga inicial crea universidades, residentes…
 * con las mismas rutas que los formularios individuales; mientras haya un lote, el interceptor
 * `loteImportacionInterceptor` marca esas peticiones para que el historial las resuma en una
 * sola fila («Importó: 120 residentes creados…») en vez de una por registro.
 *
 * El lote termina de dos formas: `finalizar()` cuando se confirmó la carga, o `cancelar()`
 * cuando el administrador la abandona; en ese caso el historial la marca como «Cancelada» y
 * conserva lo que se alcanzó a crear en los pasos.
 */
@Injectable({ providedIn: 'root' })
export class LoteImportacion {
  private readonly historialService = inject(HistorialService);
  private readonly loteActual = signal<Lote | null>(null);
  readonly actual = this.loteActual.asReadonly();

  constructor() {
    // Cerrar o recargar la pestaña no destruye los componentes: se avisa con sendBeacon, que
    // el navegador entrega aunque la página ya se esté cerrando.
    const ventana = inject(DOCUMENT).defaultView;
    const cancelarAlCerrarPestana = () => this.cancelar({ alCerrarPestana: true });
    ventana?.addEventListener('pagehide', cancelarAlCerrarPestana);
    inject(DestroyRef).onDestroy(() =>
      ventana?.removeEventListener('pagehide', cancelarAlCerrarPestana),
    );
  }

  iniciar(nombreArchivo: string) {
    this.cancelar();
    this.loteActual.set({ id: crypto.randomUUID(), nombreArchivo });
  }

  /** La carga se confirmó: el historial ya registró su resultado. */
  finalizar() {
    this.loteActual.set(null);
  }

  /** Se abandonó la importación sin confirmar. No hace nada si no hay una en curso. */
  cancelar({ alCerrarPestana = false } = {}) {
    const lote = this.loteActual();
    if (!lote) return;
    // Se limpia antes de avisar para que el aviso no viaje marcado como parte del lote.
    this.loteActual.set(null);
    const urlCancelar = this.historialService.urlCancelarImportacion(lote.id);
    if (alCerrarPestana && navigator.sendBeacon?.(urlCancelar)) return;
    // Si el aviso falla, la importación queda «En curso» en el historial; no afecta los datos.
    this.historialService.cancelarImportacion(lote.id).subscribe({ error: () => undefined });
  }
}
