/** Backend: CargaInicialRequest y CargaInicialResponse. */
export interface FilaCargaInicial {
  fila: number;
  universidad: string;
  pais: string;
  programa: string;
  residente: string;
  tipoDocumento: string;
  documento: string;
  perfilAcademico: string;
  servicio: string;
  cupoMaximo: string;
  cupoSimultaneo: string;
  asignatura: string;
  inicio: string;
  fin: string;
}
export type CampoCarga = Exclude<keyof FilaCargaInicial, 'fila'>;
export interface ResultadoCargaInicial {
  validacion: string;
  guardado: boolean;
  total: number;
  validos: number;
  advertencias: number;
  errores: number;
  omitidos: number;
  nuevos: Record<string, number>;
  filas: { fila: number; estado: string; mensaje: string }[];
}
