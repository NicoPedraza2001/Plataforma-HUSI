import { FilaImportacion } from '../../../../models/importacion-programas.model';
import { HojaExcel } from '../../../../shared/excel/excel';

/**
 * Convierte las filas de la hoja en filas de importación de programas, según la fila de
 * encabezados (base 1) y las columnas elegidas (base 0). Conserva el número de fila original
 * del Excel e ignora las filas vacías en las dos columnas.
 */
export function mapearFilas(
  hoja: HojaExcel,
  encabezado: number,
  nombre: number,
  universidad: number,
): FilaImportacion[] {
  return hoja.filas
    .slice(encabezado)
    .map((celdas, i) => ({
      fila: encabezado + i + 1,
      nombre: (celdas[nombre] ?? '').trim(),
      universidad: (celdas[universidad] ?? '').trim(),
    }))
    .filter((f) => f.nombre !== '' || f.universidad !== '');
}
