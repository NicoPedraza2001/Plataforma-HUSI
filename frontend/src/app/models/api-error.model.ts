/**
 * Cuerpo de error que devuelve el backend en cualquier respuesta 4xx.
 * Backend: `exception/ApiExceptionHandler.ErrorResponse`.
 */
export interface ApiError {
  mensaje: string;
  /** Errores de validación por campo (`nombre` → mensaje). Vacío en errores de negocio. */
  campos: Record<string, string>;
}
