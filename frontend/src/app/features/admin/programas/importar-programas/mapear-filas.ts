import { FilaImportacion } from '../../../../models/importacion-programas.model';
import { HojaExcel } from '../../../../shared/excel/excel';

/**
 * Convierte las filas de la hoja en filas de importación de programas, según la fila de
 * encabezados (base 1) y las columnas elegidas (base 0). Conserva el número de fila original
 * del Excel e ignora las filas vacías en las dos columnas.
 */
export function mapearFilas(
  hoja: HojaExcel,
  filaEncabezado: number,
  columnaNombre: number,
  columnaUniversidad: number,
): FilaImportacion[] {
  return hoja.filas
    .slice(filaEncabezado)
    .map((celdas, posicion) => ({
      fila: filaEncabezado + posicion + 1,
      nombre: (celdas[columnaNombre] ?? '').trim(),
      universidad: (celdas[columnaUniversidad] ?? '').trim(),
    }))
    .filter((fila) => fila.nombre !== '' || fila.universidad !== '');
}
