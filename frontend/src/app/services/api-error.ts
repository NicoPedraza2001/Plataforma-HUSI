import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api-error.model';

/**
 * Traduce cualquier error de una llamada al backend a un texto listo para mostrar.
 * Usa el `mensaje` y los errores por campo de `ApiError` cuando el backend los envía.
 */
export function mensajeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0 || error.status === 502 || error.status === 504) {
      return 'No pudimos conectar con el servidor. Comprueba que el backend esté encendido e intenta de nuevo.';
    }

    if (error.status >= 500 && !error.error?.mensaje) return 'El servidor no está disponible o no pudo responder. Intenta nuevamente.';
    const detalle = error.error as Partial<ApiError> | null;
    if (detalle?.mensaje) {
      return [detalle.mensaje, ...Object.values(detalle.campos ?? {})].join(' ');
    }
  }

  return 'No se pudo completar la operación. Intenta de nuevo.';
}
