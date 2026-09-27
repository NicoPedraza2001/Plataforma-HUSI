/** Contratos de respuesta y solicitudes del dominio; conserva los campos de la API. */
export interface Rotacion {
  id: string;
  residente: string;
  universidad: string;
  servicio: string;
  asignatura: string;
  doctor: string;
  inicio: string;
  fin: string;
  estado: string;
}

export interface RotacionDatos {
  asignaturaId: number;
  inicio: string;
  fin: string;
}

export interface RotacionGestion extends RotacionDatos {
  asignatura: string;
  servicio: string;
  servicioId: number;
  activa: boolean;
  disponibilidad: string;
  cupoDisponible: number;
  relaciones: number;
}

export interface CatalogosRotacion {
  asignaturas: { id: number; nombre: string; servicio: string; servicioId: number }[];
  servicios: { id: number; nombre: string }[];
}

export interface FilaRotacion {
  fila: number;
  asignatura: string;
  servicio: string;
  inicio: string;
  fin: string;
}

export interface ResultadoRotaciones {
  total: number;
  VALIDO: number;
  CREADO: number;
  OMITIDO: number;
  ERROR: number;
  filas: { fila: number; estado: string; mensaje: string }[];
}

/** Backend: dto/response/ResidenteRotacionResponse. */
export interface ResidenteRotacion {
  residenteId: number;
  nombre: string;
  documento: string;
  programa: string;
  estado: 'Asignado' | 'Esperando cupo' | 'Cancelado';
}
