import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_URL } from './api.config';
import { LoteImportacion } from './lote-importacion';

/** Backend: config/LoteImportacionFiltro. */
export const CABECERA_LOTE = 'X-Lote-Importacion';
export const CABECERA_ARCHIVO = 'X-Archivo-Importacion';

/** Agrega el lote de la importación en curso a cada petición al backend (ver `LoteImportacion`). */
export const loteImportacionInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const lote = inject(LoteImportacion).actual();
  if (!lote || !peticion.url.startsWith(API_URL)) return siguiente(peticion);
  return siguiente(
    peticion.clone({
      setHeaders: {
        [CABECERA_LOTE]: lote.id,
        // Las cabeceras solo admiten ASCII: el nombre del archivo viaja codificado como URL.
        [CABECERA_ARCHIVO]: encodeURIComponent(lote.nombreArchivo),
      },
    }),
  );
};
