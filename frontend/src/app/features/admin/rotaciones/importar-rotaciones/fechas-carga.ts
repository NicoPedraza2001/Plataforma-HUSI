/**
 * Fechas de la carga inicial. El backend exige AAAA-MM-DD. Las celdas con formato de fecha ya
 * llegan así desde `leerExcel`, pero las escritas como texto llegan tal cual («01/11/2026»),
 * y Excel guarda como texto las fechas que no existen («31/11/2026»).
 */

const ANIO_MINIMO = 1900;
const ANIO_MAXIMO = 9999;
/** 2026-11-01, 2026/11/01 o 2026.11.01 */
const ANIO_MES_DIA = /^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})$/;
/** 01/11/2026, 1-11-2026 o 01.11.2026: en Colombia el día va primero. */
const DIA_MES_ANIO = /^(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})$/;

/**
 * Devuelve la fecha en AAAA-MM-DD si el texto es una fecha real del calendario en un formato
 * reconocido; `null` si no lo es (formato desconocido o fecha inexistente como el 31 de noviembre).
 */
export function normalizarFecha(texto: string): string | null {
  const valor = texto.trim();
  const anioMesDia = ANIO_MES_DIA.exec(valor);
  const diaMesAnio = DIA_MES_ANIO.exec(valor);
  if (anioMesDia) return construirFecha(+anioMesDia[1], +anioMesDia[2], +anioMesDia[3]);
  if (diaMesAnio) return construirFecha(+diaMesAnio[3], +diaMesAnio[2], +diaMesAnio[1]);
  return null;
}

/** Explica qué está mal en las fechas de una fila; cadena vacía si son correctas. */
export function describirErrorDeFechas(inicio: string, fin: string): string {
  if (!inicio || !fin) return 'Indica la fecha de inicio y la de fin.';
  const inicioNormalizado = normalizarFecha(inicio);
  const finNormalizado = normalizarFecha(fin);
  if (!inicioNormalizado) return `La fecha de inicio «${inicio}» no es una fecha válida.`;
  if (!finNormalizado) return `La fecha de fin «${fin}» no es una fecha válida.`;
  if (finNormalizado < inicioNormalizado) {
    return 'La fecha de fin no puede ser anterior a la fecha de inicio.';
  }
  return '';
}

function construirFecha(anio: number, mes: number, dia: number): string | null {
  if (anio < ANIO_MINIMO || anio > ANIO_MAXIMO) return null;
  const fecha = new Date(Date.UTC(anio, mes - 1, dia));
  // Date corre los días sobrantes al mes siguiente (31/11 → 01/12): así se detecta que no existe.
  const existeEnElCalendario =
    fecha.getUTCFullYear() === anio &&
    fecha.getUTCMonth() === mes - 1 &&
    fecha.getUTCDate() === dia;
  if (!existeEnElCalendario) return null;
  return [anio, mes, dia]
    .map((parte, indice) => String(parte).padStart(indice ? 2 : 4, '0'))
    .join('-');
}
