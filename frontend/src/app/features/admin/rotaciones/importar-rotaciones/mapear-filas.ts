import { FilaRotacion } from '../../../../models/rotacion.model';

/** Convierte las columnas elegidas en filas para validar en el backend. */
export function mapearFilas(
  filas: string[][],
  encabezado: number,
  columnas: number[],
): FilaRotacion[] {
  return filas
    .slice(encabezado)
    .map((f, i) => ({
      fila: encabezado + i + 1,
      asignatura: (f[columnas[0]] ?? '').trim(),
      servicio: (f[columnas[1]] ?? '').trim(),
      inicio: (f[columnas[2]] ?? '').trim(),
      fin: (f[columnas[3]] ?? '').trim(),
    }))
    .filter((f) => f.asignatura || f.servicio || f.inicio || f.fin);
}
