import { CampoCarga, FilaCargaInicial } from '../../../../models/carga-inicial.model';
export const CAMPOS_CARGA: {
  clave: CampoCarga;
  nombre: string;
  max: number;
  alias: string[];
  ayuda?: string;
}[] = [
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
export const normalizar = (valor: string) =>
  valor
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
export function detectarColumnas(encabezados: string[]): number[] {
  const nombres = encabezados.map(normalizar);
  return CAMPOS_CARGA.map((c) =>
    nombres.findIndex((n) => c.alias.includes(n) || normalizar(c.nombre) === n),
  );
}
export function prepararCarga(
  filas: string[][],
  encabezado: number,
  columnas: number[],
  comunes: string[],
): FilaCargaInicial[] {
  return filas
    .slice(encabezado)
    .map((f, i) => ({ f, i }))
    .filter(({ f }) => columnas.some((c) => c >= 0 && (f[c] ?? '').trim()))
    .map(({ f, i }) => {
      const datos = { fila: encabezado + i + 1 } as FilaCargaInicial;
      CAMPOS_CARGA.forEach(
        (campo, n) =>
          (datos[campo.clave] = (
            columnas[n] >= 0
              ? (f[columnas[n]] ?? '').trim() || (comunes[n] ?? '')
              : (comunes[n] ?? '')
          ).trim()),
      );
      return datos;
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
  const disponibles = new Set(encabezados.map(normalizar));
  return PLANTILLA_FINAL.every((c) => disponibles.has(normalizar(c)));
}
