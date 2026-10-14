/**
 * Lectura y generación de archivos Excel (.xlsx) en el navegador, reutilizable por cualquier
 * pantalla con importación o exportación. ExcelJS se carga bajo demanda para no aumentar la
 * carga inicial de la app.
 */
import type { Workbook } from 'exceljs';

export interface HojaExcel {
  nombre: string;
  /** Celdas como texto; `filas[0]` es la fila 1 del Excel. */
  filas: string[][];
}

/**
 * Texto que reemplaza a las celdas con fórmula: no se ejecutan fórmulas ni se confía en
 * valores calculados obsoletos. La pantalla debe rechazar las filas que lo contengan.
 */
export const MARCA_FORMULA = '[FÓRMULA: reemplazar por texto]';

/** Índice de columna (base 0) a letra de Excel: 0 → A, 26 → AA. */
export function letraColumna(indice: number): string {
  let letra = '';
  let numero = indice + 1;
  while (numero > 0) {
    numero--;
    letra = String.fromCharCode(65 + (numero % 26)) + letra;
    numero = Math.floor(numero / 26);
  }
  return letra;
}

/** Lee todas las hojas de un .xlsx. Lanza `Error` con un mensaje listo para mostrar. */
export async function leerExcel(archivo: File, maxFilas = 1100): Promise<HojaExcel[]> {
  if (!/\.xlsx$/i.test(archivo.name))
    throw new Error(
      'Selecciona un archivo .xlsx. Si es .xls, guárdalo como Libro de Excel (.xlsx).',
    );
  if (archivo.size > 5 * 1024 * 1024)
    throw new Error('El archivo supera 5 MB. Divide los datos en archivos más pequeños.');
  const { Workbook } = await import('exceljs');
  const libro = new Workbook();
  try {
    await libro.xlsx.load(await archivo.arrayBuffer());
  } catch {
    throw new Error(
      'No se pudo leer el Excel. Comprueba que no esté dañado ni protegido con contraseña.',
    );
  }
  if (!libro.worksheets.length) throw new Error('El libro no contiene hojas.');
  return libro.worksheets.map((hoja) => {
    if (hoja.rowCount > maxFilas || hoja.columnCount > 100)
      throw new Error(
        `La hoja supera ${maxFilas} filas o 100 columnas. Divide el archivo o elimina filas y columnas sobrantes.`,
      );
    const filas: string[][] = [];
    for (let numeroFila = 1; numeroFila <= hoja.rowCount; numeroFila++) {
      const celdas: string[] = [];
      for (let numeroColumna = 1; numeroColumna <= hoja.columnCount; numeroColumna++) {
        const celda = hoja.getCell(numeroFila, numeroColumna);
        celdas.push(
          celda.formula
            ? MARCA_FORMULA
            : celda.value instanceof Date
              ? celda.value.toISOString().slice(0, 10)
              : celda.text.trim(),
        );
      }
      filas.push(celdas);
    }
    return { nombre: hoja.name, filas };
  });
}

/** Hoja de un libro descargable: encabezados en la fila 1 y luego las filas. */
export interface HojaDescarga {
  /** Excel admite hasta 31 caracteres y no acepta \ / ? * : [ ]. */
  nombre: string;
  columnas: string[];
  filas: (string | number)[][];
  /** Ancho fijo de todas las columnas; sin él, cada columna se ajusta a su texto más largo. */
  ancho?: number;
}

/** Genera un .xlsx de una hoja (encabezados en negrita) y lo descarga. */
export async function descargarLibro(
  nombreArchivo: string,
  nombreHoja: string,
  columnas: string[],
  filas: (string | number)[][],
): Promise<void> {
  await descargarHojas(nombreArchivo, [{ nombre: nombreHoja, columnas, filas, ancho: 35 }]);
}

/** Genera un .xlsx con varias hojas (encabezados en negrita) y lo descarga. */
export async function descargarHojas(nombreArchivo: string, hojas: HojaDescarga[]): Promise<void> {
  const { Workbook } = await import('exceljs');
  const libro: Workbook = new Workbook();
  hojas.forEach(({ nombre, columnas, filas, ancho }) => {
    const hoja = libro.addWorksheet(nombre);
    hoja.addRow(columnas);
    filas.forEach((fila) => hoja.addRow(fila));
    hoja.getRow(1).font = { bold: true };
    hoja.columns.forEach((columna, indice) => {
      const largo = Math.max(
        ...[columnas, ...filas].map((fila) => String(fila[indice] ?? '').length),
      );
      columna.width = ancho ?? Math.min(60, Math.max(10, largo + 2));
    });
  });
  const buffer = await libro.xlsx.writeBuffer();
  const url = URL.createObjectURL(
    new Blob([new Uint8Array(buffer)], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    }),
  );
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo;
  enlace.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
