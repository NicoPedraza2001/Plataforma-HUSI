import { Injectable, signal } from '@angular/core';

export interface Lote {
  id: string;
  nombreArchivo: string;
}

/**
 * Importación de Excel en curso. El asistente de carga inicial crea universidades, residentes…
 * con las mismas rutas que los formularios individuales; mientras haya un lote, el interceptor
 * `loteImportacionInterceptor` marca esas peticiones para que el historial las resuma en una
 * sola fila («Importó: 120 residentes creados…») en vez de una por registro.
 */
@Injectable({ providedIn: 'root' })
export class LoteImportacion {
  private readonly loteActual = signal<Lote | null>(null);
  readonly actual = this.loteActual.asReadonly();

  iniciar(nombreArchivo: string) {
    this.loteActual.set({ id: crypto.randomUUID(), nombreArchivo });
  }

  terminar() {
    this.loteActual.set(null);
  }
}
