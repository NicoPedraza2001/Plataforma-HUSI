/**
 * Utilidades compartidas por los pasos de emparejamiento (universidades, programas,
 * residentes): agrupan las filas del Excel, comparan sus valores con la base de datos y
 * reescriben las filas cuando el administrador corrige un dato.
 */
import { CampoCarga, FilaCargaInicial } from '../../../../models/carga-inicial.model';
import { normalizar } from './campos-carga';

/** Situación de un dato del Excel frente a lo registrado en la base de datos. */
export type EstadoEmparejamiento =
  'encontrado' | 'nuevo' | 'diferencias' | 'inactivo' | 'incompleto';

export const ETIQUETA_ESTADO: Record<EstadoEmparejamiento, string> = {
  encontrado: 'Existe',
  nuevo: 'Nuevo',
  diferencias: 'Con diferencias',
  inactivo: 'Inactivo',
  incompleto: 'Incompleto',
};

/** Clase global de `styles/admin.css` que colorea el badge de cada estado. */
export const CLASE_BADGE_ESTADO: Record<EstadoEmparejamiento, string> = {
  encontrado: 'badge--activo',
  nuevo: 'badge--info',
  diferencias: 'badge--pendiente',
  inactivo: 'badge--inactivo',
  incompleto: 'badge--inactivo',
};

/** Opciones del filtro por estado que comparten las tablas de emparejamiento. */
export type FiltroEstado = 'todos' | 'pendientes' | EstadoEmparejamiento;

export const OPCIONES_FILTRO_ESTADO: readonly { valor: FiltroEstado; etiqueta: string }[] = [
  { valor: 'pendientes', etiqueta: 'Pendientes por resolver' },
  { valor: 'todos', etiqueta: 'Todos' },
  { valor: 'nuevo', etiqueta: 'Nuevos' },
  { valor: 'diferencias', etiqueta: 'Con diferencias' },
  { valor: 'incompleto', etiqueta: 'Incompletos' },
  { valor: 'inactivo', etiqueta: 'Inactivos' },
  { valor: 'encontrado', etiqueta: 'Ya existen' },
];

export function cumpleFiltroEstado(
  elemento: { estado: EstadoEmparejamiento },
  filtro: FiltroEstado,
): boolean {
  if (filtro === 'todos') return true;
  if (filtro === 'pendientes') return estaPendiente(elemento);
  return elemento.estado === filtro;
}

/** Registro existente en la base de datos que se puede asociar a un valor del Excel. */
export interface RegistroDelSistema {
  id: number;
  nombre: string;
  estado: string;
}

/**
 * Valor del Excel (una universidad o un programa) con las filas donde aparece y su
 * situación frente a la base de datos.
 */
export interface GrupoEmparejamiento {
  clave: string;
  /** Texto que tienen hoy las filas (ya corregido si el administrador lo cambió). */
  valorEnExcel: string;
  /** Cómo venía escrito en el archivo, si difiere del valor actual. */
  valoresOriginales: string[];
  /** Dato que acompaña al valor: el país de la universidad o la universidad del programa. */
  contexto: string;
  /** Id del registro de contexto (la universidad del programa), si existe. */
  contextoId: number | null;
  numerosFila: number[];
  estado: EstadoEmparejamiento;
  registro: RegistroDelSistema | null;
  observaciones: string[];
  /** Registros parecidos, del más al menos similar. */
  sugerencias: RegistroDelSistema[];
  /** Registros activos que el administrador puede elegir para asociar. */
  opciones: RegistroDelSistema[];
  /** Falso cuando falta un dato previo (p. ej. un programa sin universidad registrada). */
  sePuedeCrear: boolean;
}

/** Igualdad que aplica el backend al buscar por nombre: ignora mayúsculas y espacios extremos. */
export function esMismoTexto(textoA: string, textoB: string): boolean {
  return claveDeTexto(textoA) === claveDeTexto(textoB);
}

/** Clave para agrupar filas con la misma igualdad que usa el backend. */
export function claveDeTexto(texto: string): string {
  return texto.trim().toLowerCase();
}

/** Texto para buscar en las tablas del asistente: sin tildes ni mayúsculas. */
export function normalizarBusqueda(texto: string): string {
  return normalizar(texto);
}

function prepararParaComparar(texto: string): string {
  return normalizar(texto)
    .replace(/[^a-z0-9ñ]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function obtenerBigramas(texto: string): string[] {
  const bigramas: string[] = [];
  for (let posicion = 0; posicion < texto.length - 1; posicion++) {
    bigramas.push(texto.slice(posicion, posicion + 2));
  }
  return bigramas;
}

/** Palabras que no distinguen un nombre de otro: se ignoran al comparar por palabras. */
const PALABRAS_GENERICAS = new Set([
  'de',
  'del',
  'la',
  'las',
  'los',
  'el',
  'y',
  'en',
  'universidad',
  'univ',
  'u',
  'programa',
  'especializacion',
  'esp',
]);

function obtenerPalabrasDistintivas(textoComparable: string): string[] {
  return textoComparable
    .split(' ')
    .filter((palabra) => palabra.length > 1 && !PALABRAS_GENERICAS.has(palabra));
}

/** Coeficiente de Dice sobre pares de letras: tolera errores de digitación. */
function compararPorLetras(comparableA: string, comparableB: string): number {
  const bigramasA = obtenerBigramas(comparableA);
  const bigramasPendientesB = obtenerBigramas(comparableB);
  const totalBigramas = bigramasA.length + bigramasPendientesB.length;
  if (!totalBigramas) return 0;
  let coincidencias = 0;
  for (const bigrama of bigramasA) {
    const posicionEnB = bigramasPendientesB.indexOf(bigrama);
    if (posicionEnB >= 0) {
      coincidencias++;
      bigramasPendientesB.splice(posicionEnB, 1);
    }
  }
  return (2 * coincidencias) / totalBigramas;
}

/**
 * Proporción de palabras distintivas del texto más corto que aparecen (completas o como
 * abreviatura) en el más largo. Reconoce «Javeriana» o «Univ. Javeriana» dentro de
 * «Pontificia Universidad Javeriana», que por letras se parecen poco.
 */
function compararPorPalabras(comparableA: string, comparableB: string): number {
  const [palabrasCortas, palabrasLargas] = [
    obtenerPalabrasDistintivas(comparableA),
    obtenerPalabrasDistintivas(comparableB),
  ].sort((a, b) => a.length - b.length);
  if (!palabrasCortas.length) return 0;
  const palabrasEncontradas = palabrasCortas.filter((palabraCorta) =>
    palabrasLargas.some(
      (palabraLarga) =>
        palabraLarga === palabraCorta ||
        (palabraCorta.length >= 3 && palabraLarga.startsWith(palabraCorta)),
    ),
  );
  return palabrasEncontradas.length / palabrasCortas.length;
}

/**
 * Parecido entre dos textos de 0 (nada) a 1 (iguales sin contar tildes, mayúsculas ni
 * signos). Toma la mejor de dos medidas: por letras (errores de digitación) y por
 * palabras (nombres abreviados o incompletos).
 */
export function calcularSimilitud(textoA: string, textoB: string): number {
  const comparableA = prepararParaComparar(textoA);
  const comparableB = prepararParaComparar(textoB);
  if (!comparableA || !comparableB) return 0;
  if (comparableA === comparableB) return 1;
  // Por letras se comparan solo las palabras distintivas: «Universidad …» no debe parecerse
  // a otra universidad solo por compartir esa palabra.
  const distintivasA = obtenerPalabrasDistintivas(comparableA).join(' ');
  const distintivasB = obtenerPalabrasDistintivas(comparableB).join(' ');
  const similitudPorLetras = compararPorLetras(distintivasA, distintivasB);
  const similitudPorPalabras = 0.9 * compararPorPalabras(comparableA, comparableB);
  return Math.max(similitudPorLetras, similitudPorPalabras);
}

/** Diferencia máxima con la mejor sugerencia: si hay una casi exacta, no se ofrecen las flojas. */
const MARGEN_FRENTE_A_LA_MEJOR = 0.25;

/** Registros con nombre parecido al valor del Excel, ordenados del más al menos similar. */
export function buscarSugerencias<T extends { nombre: string }>(
  valorEnExcel: string,
  registros: readonly T[],
  limite = 3,
  similitudMinima = 0.45,
): T[] {
  const candidatos = registros
    .map((registro) => ({ registro, similitud: calcularSimilitud(valorEnExcel, registro.nombre) }))
    .filter(({ similitud }) => similitud >= similitudMinima)
    .sort((a, b) => b.similitud - a.similitud);
  const mejorSimilitud = candidatos[0]?.similitud ?? 0;
  return candidatos
    .filter(({ similitud }) => similitud >= mejorSimilitud - MARGEN_FRENTE_A_LA_MEJOR)
    .slice(0, limite)
    .map(({ registro }) => registro);
}

/** Agrupa las filas por una clave, conservando el orden de aparición en el Excel. */
export function agruparFilas(
  filas: readonly FilaCargaInicial[],
  obtenerClave: (fila: FilaCargaInicial) => string,
): Map<string, FilaCargaInicial[]> {
  const grupos = new Map<string, FilaCargaInicial[]>();
  for (const fila of filas) {
    const clave = obtenerClave(fila);
    const filasDelGrupo = grupos.get(clave);
    if (filasDelGrupo) filasDelGrupo.push(fila);
    else grupos.set(clave, [fila]);
  }
  return grupos;
}

/** Valores no vacíos y distintos (con la igualdad del backend) de un campo en varias filas. */
export function valoresDistintos(filas: readonly FilaCargaInicial[], campo: CampoCarga): string[] {
  const valores: string[] = [];
  for (const fila of filas) {
    const valor = fila[campo].trim();
    if (valor && !valores.some((existente) => esMismoTexto(existente, valor))) valores.push(valor);
  }
  return valores;
}

/** Cómo venía escrito un campo en el archivo original, si difiere del valor actual. */
export function buscarValoresOriginales(
  filasOriginalesPorNumero: ReadonlyMap<number, FilaCargaInicial>,
  numerosFila: readonly number[],
  campo: CampoCarga,
  valorActual: string,
): string[] {
  const filasOriginales = numerosFila
    .map((numeroFila) => filasOriginalesPorNumero.get(numeroFila))
    .filter((fila): fila is FilaCargaInicial => !!fila);
  return valoresDistintos(filasOriginales, campo).filter(
    (valorOriginal) => valorOriginal !== valorActual.trim(),
  );
}

export function indexarPorNumeroDeFila(
  filas: readonly FilaCargaInicial[],
): Map<number, FilaCargaInicial> {
  return new Map(filas.map((fila) => [fila.fila, fila]));
}

/** Copia de las filas con los cambios aplicados solo a las filas indicadas. */
export function actualizarFilas(
  filas: readonly FilaCargaInicial[],
  numerosFila: readonly number[],
  cambios: Partial<Omit<FilaCargaInicial, 'fila'>>,
): FilaCargaInicial[] {
  const filasACambiar = new Set(numerosFila);
  return filas.map((fila) => (filasACambiar.has(fila.fila) ? { ...fila, ...cambios } : fila));
}

/** Copia de las filas sin las indicadas: el administrador decidió no importarlas. */
export function excluirFilas(
  filas: readonly FilaCargaInicial[],
  numerosFila: readonly number[],
): FilaCargaInicial[] {
  const filasAExcluir = new Set(numerosFila);
  return filas.filter((fila) => !filasAExcluir.has(fila.fila));
}

/** Pone primero lo que requiere atención y deja al final lo que ya existe. */
export function ordenarPendientesPrimero<T extends { estado: EstadoEmparejamiento }>(
  elementos: T[],
): T[] {
  const prioridad: Record<EstadoEmparejamiento, number> = {
    incompleto: 0,
    inactivo: 1,
    diferencias: 2,
    nuevo: 3,
    encontrado: 4,
  };
  return [...elementos].sort((a, b) => prioridad[a.estado] - prioridad[b.estado]);
}

export function estaPendiente(elemento: { estado: EstadoEmparejamiento }): boolean {
  return elemento.estado !== 'encontrado';
}

/** Lista de números de fila legible: «2, 3, 7 y 4 más». */
export function describirFilas(numerosFila: readonly number[], maximoVisible = 5): string {
  const visibles = numerosFila.slice(0, maximoVisible).join(', ');
  const restantes = numerosFila.length - maximoVisible;
  return restantes > 0 ? `${visibles} y ${restantes} más` : visibles;
}

/**
 * Ejecuta una acción sobre varios elementos en orden (p. ej. crear registros nuevos en
 * bloque). Si uno falla, sigue con los demás y devuelve los errores para mostrarlos juntos.
 */
export async function procesarEnLote<T>(
  elementos: readonly T[],
  procesar: (elemento: T) => Promise<void>,
  describir: (elemento: T) => string,
  convertirError: (error: unknown) => string,
): Promise<{ procesados: number; errores: string[] }> {
  const errores: string[] = [];
  for (const elemento of elementos) {
    try {
      await procesar(elemento);
    } catch (error) {
      errores.push(`${describir(elemento)}: ${convertirError(error)}`);
    }
  }
  return { procesados: elementos.length - errores.length, errores };
}

/** Cupo del Excel como entero no negativo, igual que lo exige el backend; `null` si no es válido. */
export function leerCupo(texto: string): number | null {
  const valor = texto.trim();
  return /^[0-9]+$/.test(valor) && Number(valor) <= 2147483647 ? Number(valor) : null;
}

/** Elige la forma singular o plural según la cantidad: (1, 'fila', 'filas') → 'fila'. */
export function segunCantidad(cantidad: number, singular: string, plural: string): string {
  return cantidad === 1 ? singular : plural;
}

/** Cantidad con su palabra concordada: (1, 'fila', 'filas') → '1 fila'; (3, …) → '3 filas'. */
export function cantidadConPalabra(cantidad: number, singular: string, plural: string): string {
  return `${cantidad} ${segunCantidad(cantidad, singular, plural)}`;
}
