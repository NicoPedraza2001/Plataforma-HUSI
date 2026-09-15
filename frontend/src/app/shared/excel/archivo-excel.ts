import { descargarLibro as descargar } from './excel';
export { leerExcel, type HojaExcel } from './excel';
export function descargarLibro(nombre: string, columnas: string[], filas: (string | number)[][], nombreHoja = 'Programas') { return descargar(nombre, nombreHoja, columnas, filas); }
