import type { Workbook } from 'exceljs';
import { FilaImportacion } from './programas-api';
export interface HojaExcel {
  nombre: string;
  filas: string[][];
}
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
export async function leerExcel(file: File): Promise<HojaExcel[]> {
  if (!/\.xlsx$/i.test(file.name))
    throw new Error(
      'Selecciona un archivo .xlsx. Si es .xls, guárdalo como Libro de Excel (.xlsx).',
    );
  if (file.size > 5 * 1024 * 1024)
    throw new Error('El archivo supera 5 MB. Divide los datos en archivos más pequeños.');
  const { Workbook } = await import('exceljs');
  const libro = new Workbook();
  try {
    await libro.xlsx.load(await file.arrayBuffer());
  } catch {
    throw new Error(
      'No se pudo leer el Excel. Comprueba que no esté dañado ni protegido con contraseña.',
    );
  }
  if (!libro.worksheets.length) throw new Error('El libro no contiene hojas.');
  return libro.worksheets.map((hoja) => {
    if (hoja.rowCount > 1100 || hoja.columnCount > 100)
      throw new Error(
        'La hoja supera 1100 filas o 100 columnas. Usa un archivo con hasta 1000 registros y elimina filas o columnas sobrantes.',
      );
    const filas: string[][] = [];
    for (let i = 1; i <= hoja.rowCount; i++) {
      const celdas: string[] = [];
      for (let j = 1; j <= hoja.columnCount; j++) {
        const celda = hoja.getCell(i, j);
        // No ejecutar fórmulas ni confiar en valores calculados obsoletos.
        celdas.push(celda.formula ? '[FÓRMULA: reemplazar por texto]' : celda.text.trim());
      }
      filas.push(celdas);
    }
    return { nombre: hoja.name, filas };
  });
}
export async function descargarLibro(
  nombre: string,
  columnas: string[],
  filas: (string | number)[][],
): Promise<void> {
  const { Workbook } = await import('exceljs');
  const libro: Workbook = new Workbook();
  const hoja = libro.addWorksheet('Programas');
  hoja.addRow(columnas);
  filas.forEach((f) => hoja.addRow(f));
  hoja.getRow(1).font = { bold: true };
  hoja.columns.forEach((c) => {
    c.width = 35;
  });
  const buffer = await libro.xlsx.writeBuffer();
  const url = URL.createObjectURL(
    new Blob([new Uint8Array(buffer)], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    }),
  );
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
