import { CampoCarga, FilaCargaInicial } from '../../../../models/carga-inicial.model';
import { HojaExcel } from '../../../../shared/excel/excel';
import { normalizarFecha } from './fechas-carga';

/** Campo de la carga inicial que el administrador relaciona con una columna del Excel. */
export interface CampoDeCarga {
  clave: CampoCarga;
  nombre: string;
  /** Longitud máxima que acepta el backend. */
  max: number;
  /** Encabezados del Excel (normalizados) que se reconocen automáticamente. */
  alias: string[];
  ayuda?: string;
}

export const CAMPOS_CARGA: CampoDeCarga[] = [
  {
    clave: 'universidad',
    nombre: 'Universidad',
    max: 150,
    alias: ['universidad'],
    ayuda: 'Si el archivo pertenece a una sola universidad, escribe su nombre como valor común.',
  },
  {
    clave: 'pais',
    nombre: 'País',
    max: 100,
    alias: ['pais'],
    ayuda: 'Obligatorio para crear universidades nuevas.',
  },
  { clave: 'programa', nombre: 'Programa', max: 150, alias: ['programa'] },
  {
    clave: 'residente',
    nombre: 'Nombre del residente',
    max: 200,
    alias: ['residente', 'nombre', 'nombre completo', 'name'],
  },
  {
    clave: 'tipoDocumento',
    nombre: 'Tipo de documento',
    max: 30,
    alias: ['tipo documento', 'tipo de documento'],
    ayuda:
      'Código del catálogo (CC, TI, CE, PA, RUT, PEP, PPT) o su nombre; no se deduce del número.',
  },
  {
    clave: 'documento',
    nombre: 'Documento',
    max: 50,
    alias: ['documento', 'numero documento', 'numero de documento', 'documento2'],
  },
  {
    clave: 'perfilAcademico',
    nombre: 'Perfil académico',
    max: 100,
    alias: ['perfil academico', 'perfil'],
    ayuda: 'Opcional: Pregrado o Posgrado.',
  },
  { clave: 'servicio', nombre: 'Servicio', max: 150, alias: ['servicio', 'servicio_husi'] },
  {
    clave: 'cupoMaximo',
    nombre: 'Cupo máximo instalado',
    max: 10,
    alias: ['cupo maximo instalado', 'cupo maximo'],
    ayuda: 'Solo para servicios nuevos. No se calcula contando residentes.',
  },
  {
    clave: 'cupoSimultaneo',
    nombre: 'Cupo simultáneo',
    max: 10,
    alias: ['cupo simultaneo'],
    ayuda: 'Solo para servicios nuevos; no debe superar el máximo instalado.',
  },
  {
    clave: 'asignatura',
    nombre: 'Asignatura',
    max: 200,
    alias: ['asignatura', 'rotaciones /asignaturas', 'rotaciones/asignaturas'],
  },
  {
    clave: 'inicio',
    nombre: 'Fecha de inicio',
    max: 10,
    alias: ['inicio', 'fecha inicio', 'fecha de inicio', 'fecha inicial'],
  },
  {
    clave: 'fin',
    nombre: 'Fecha de fin',
    max: 10,
    alias: ['fin', 'fecha fin', 'fecha de fin', 'fecha final'],
  },
];

/** Máximo de filas de datos que acepta la carga inicial. */
export const MAXIMO_FILAS_CARGA = 5000;

/** Archivo leído en el paso «Archivo». */
export interface LibroExcel {
  nombreArchivo: string;
  hojas: HojaExcel[];
}

/**
 * Cómo se leen las filas del Excel: qué hoja, en qué fila están los encabezados y qué
 * columna (o valor común) alimenta cada campo de `CAMPOS_CARGA`, en el mismo orden.
 */
export interface ConfiguracionColumnas {
  indiceHoja: number;
  /** Número de fila (base 1, como en Excel) que contiene los encabezados. */
  filaEncabezados: number;
  /** Índice de columna (base 0) por campo; -1 cuando el campo no viene en una columna. */
  columnaPorCampo: number[];
  /** Valor que completa las celdas vacías del campo o lo reemplaza si no hay columna. */
  valorComunPorCampo: string[];
}

export const normalizar = (valor: string) =>
  valor.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

export function detectarColumnas(encabezados: string[]): number[] {
  const encabezadosNormalizados = encabezados.map(normalizar);
  return CAMPOS_CARGA.map((campo) =>
    encabezadosNormalizados.findIndex(
      (encabezado) => campo.alias.includes(encabezado) || normalizar(campo.nombre) === encabezado,
    ),
  );
}

/** Valores comunes por defecto: el tipo de documento más habitual es la cédula (CC). */
export function crearValoresComunesIniciales(): string[] {
  return CAMPOS_CARGA.map((campo) => (campo.clave === 'tipoDocumento' ? 'CC' : ''));
}

export function obtenerEncabezados(
  hojas: readonly HojaExcel[],
  indiceHoja: number,
  filaEncabezados: number,
): string[] {
  return hojas[indiceHoja]?.filas[filaEncabezados - 1] ?? [];
}

/** Configuración sugerida al abrir un archivo: primera hoja, encabezados en la fila 1. */
export function crearConfiguracionInicial(hojas: readonly HojaExcel[]): ConfiguracionColumnas {
  return {
    indiceHoja: 0,
    filaEncabezados: 1,
    columnaPorCampo: detectarColumnas(obtenerEncabezados(hojas, 0, 1)),
    valorComunPorCampo: crearValoresComunesIniciales(),
  };
}

export function sonConfiguracionesIguales(
  configuracionA: ConfiguracionColumnas | null,
  configuracionB: ConfiguracionColumnas | null,
): boolean {
  return JSON.stringify(configuracionA) === JSON.stringify(configuracionB);
}

/**
 * Convierte las celdas del Excel en filas de carga. Omite las filas sin datos en las
 * columnas elegidas y conserva el número de fila original del Excel. Las fechas escritas como
 * texto («01/11/2026») pasan a AAAA-MM-DD; las que no existen se dejan igual para que la
 * validación las señale y se corrijan en el paso Rotaciones.
 */
export function prepararCarga(
  celdasPorFila: string[][],
  filaEncabezados: number,
  columnaPorCampo: number[],
  valorComunPorCampo: string[],
): FilaCargaInicial[] {
  const tieneDatos = (celdas: string[]) =>
    columnaPorCampo.some((columna) => columna >= 0 && (celdas[columna] ?? '').trim());
  return celdasPorFila
    .slice(filaEncabezados)
    .map((celdas, posicion) => ({ celdas, numeroFilaExcel: filaEncabezados + posicion + 1 }))
    .filter(({ celdas }) => tieneDatos(celdas))
    .map(({ celdas, numeroFilaExcel }) => {
      const filaCarga = { fila: numeroFilaExcel } as FilaCargaInicial;
      CAMPOS_CARGA.forEach((campo, indiceCampo) => {
        const columna = columnaPorCampo[indiceCampo];
        const valorComun = valorComunPorCampo[indiceCampo] ?? '';
        const valorCelda = columna >= 0 ? (celdas[columna] ?? '').trim() : '';
        const valor = (valorCelda || valorComun).trim();
        const esFecha = campo.clave === 'inicio' || campo.clave === 'fin';
        filaCarga[campo.clave] = esFecha ? (normalizarFecha(valor) ?? valor) : valor;
      });
      return filaCarga;
    });
}

/** Encabezados de la plantilla institucional recibida; categoría y sede son informativos. */
export const PLANTILLA_FINAL = [
  'Categoría',
  'Programa',
  'Documento',
  'Name',
  'Perfil',
  'Universidad',
  'País',
  'Rotaciones /asignaturas',
  'id_sede',
  'Servicio_HUSI',
  'Fecha inicial',
  'Fecha final',
];

export function esPlantillaFinal(encabezados: string[]): boolean {
  const encabezadosDisponibles = new Set(encabezados.map(normalizar));
  return PLANTILLA_FINAL.every((encabezado) => encabezadosDisponibles.has(normalizar(encabezado)));
}
