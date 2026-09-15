/** Fila leída del Excel. Backend: `dto/request/ImportacionProgramasRequest.Fila`. */
export interface FilaImportacion {
  /** Número de fila original en el Excel, para que el usuario la ubique. */
  fila: number;
  nombre: string;
  universidad: string;
}

/** Backend: `dto/request/ImportacionProgramasRequest`. */
export interface ImportacionProgramasRequest {
  filas: FilaImportacion[];
}

/** VALIDO solo aparece al validar; CREADO solo al importar. */
export type EstadoFilaImportacion = 'VALIDO' | 'CREADO' | 'OMITIDO' | 'ERROR';

/** Backend: `dto/response/ImportacionProgramasResponse.Fila`. */
export interface ResultadoFila extends FilaImportacion {
  estado: EstadoFilaImportacion;
  mensaje: string;
  programaId?: number;
}

/** Backend: `dto/response/ImportacionProgramasResponse`. */
export interface ResultadoImportacion {
  total: number;
  validos: number;
  creados: number;
  omitidos: number;
  errores: number;
  filas: ResultadoFila[];
}
