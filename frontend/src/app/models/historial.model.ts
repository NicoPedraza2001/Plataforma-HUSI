/** Backend: fila del listado GET /historial (repository/query/ListadoSql, dominio «historial»). */
export interface RegistroHistorial {
  id: number;
  fecha: string;
  usuario: string;
  modulo: string;
  accion: string;
  registro: string;
  detalle: string;
  /** La fila resume una importación completa (no un registro individual). */
  esImportacion: boolean;
}

/**
 * Backend: mapper/FotografiaRegistro.datos. Campo legible («Nombre», «País»…) → valor del
 * registro en ese momento. En una importación son las cantidades del resumen.
 */
export type DatosRegistro = Record<string, string | number | boolean | null>;

/** Backend: dto/response/HistorialDetalleResponse. */
export interface DetalleMovimiento {
  id: number;
  fecha: string;
  usuario: string;
  correoUsuario: string;
  modulo: string;
  accion: string;
  registro?: string;
  detalle?: string;
  /** Cómo estaba el registro antes del cambio; no viene al crear. */
  datosAnteriores?: DatosRegistro;
  /** Cómo quedó el registro; no viene al eliminar. */
  datosNuevos?: DatosRegistro;
  esImportacion: boolean;
}
