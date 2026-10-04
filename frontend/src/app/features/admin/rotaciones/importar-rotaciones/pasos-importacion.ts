import { normalizar } from './campos-carga';

/** Identificador de cada viñeta del asistente, en el orden del diagrama entidad-relación. */
export type IdPasoImportacion =
  | 'archivo'
  | 'columnas'
  | 'universidades'
  | 'programas'
  | 'servicios'
  | 'asignaturas'
  | 'residentes'
  | 'rotaciones'
  | 'confirmacion';

export interface PasoImportacion {
  id: IdPasoImportacion;
  titulo: string;
  /** Los pasos de emparejamiento cuentan pendientes y bloquean el avance hasta resolverlos. */
  esEmparejamiento: boolean;
}

export const PASOS_IMPORTACION: readonly PasoImportacion[] = [
  { id: 'archivo', titulo: 'Archivo', esEmparejamiento: false },
  { id: 'columnas', titulo: 'Columnas', esEmparejamiento: false },
  { id: 'universidades', titulo: 'Universidades', esEmparejamiento: true },
  { id: 'programas', titulo: 'Programas', esEmparejamiento: true },
  { id: 'servicios', titulo: 'Servicios', esEmparejamiento: true },
  { id: 'asignaturas', titulo: 'Asignaturas', esEmparejamiento: true },
  { id: 'residentes', titulo: 'Residentes', esEmparejamiento: true },
  { id: 'rotaciones', titulo: 'Rotaciones', esEmparejamiento: false },
  { id: 'confirmacion', titulo: 'Confirmar', esEmparejamiento: false },
];

/**
 * El asistente crea universidades, programas, servicios, asignaturas y residentes en sus
 * propios pasos, con revisión del administrador. Por eso la validación final le pide al
 * backend que NO cree faltantes: si aparece uno (p. ej. por una corrección manual en
 * Rotaciones), se reporta como error y se envía a corregir a su paso.
 */
export const CREAR_FALTANTES_EN_LA_VALIDACION = false;

/** Hay algo que guardar: filas con cupo o filas que quedarán en lista de espera. */
export function hayQueGuardar(resultado: { validos: number; espera: number } | null): boolean {
  return !!resultado && resultado.validos + resultado.espera > 0;
}

/**
 * Paso del asistente donde se corrige un error devuelto por la validación final del
 * backend (`CargaInicialPlan`). Se decide por las palabras del mensaje; `null` cuando
 * el error no pertenece a ningún paso concreto.
 */
export function buscarPasoParaCorregir(mensajeDeError: string): IdPasoImportacion | null {
  const mensaje = normalizar(mensajeDeError);
  const contieneAlguna = (...palabras: string[]) =>
    palabras.some((palabra) => mensaje.includes(palabra));
  // «El programa del residente…» pertenece al residente: se revisa antes que «programa».
  if (contieneAlguna('residente', 'documento', 'perfil')) return 'residentes';
  if (contieneAlguna('universidad', 'pais')) return 'universidades';
  if (contieneAlguna('programa')) return 'programas';
  if (contieneAlguna('asignatura')) return 'asignaturas';
  if (contieneAlguna('servicio', 'cupo')) return 'servicios';
  if (contieneAlguna('fecha', 'rotacion')) return 'rotaciones';
  return null;
}
