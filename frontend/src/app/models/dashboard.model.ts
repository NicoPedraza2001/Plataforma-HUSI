/** Contratos de respuesta y solicitudes del dominio; conserva los campos de la API. */
/** Backend: repository/DashboardRepository (GET /dashboard). */
export interface DashboardDatos {
  indicadores: {
    id: string;
    etiqueta: string;
    valor: string;
    detalle: string;
    delta?: string;
    tono: string;
  }[];
  situacionPorPrograma: SituacionPrograma[];
  cargaResidentes: { etiqueta: string; valor: number }[];
  mapaOcupacion: MapaOcupacion;
  rotacionesPorServicio: { etiqueta: string; valor: number; maximo: number }[];
  distribucionUniversidad: { etiqueta: string; porcentaje: number }[];
  tendenciaInicios: { etiqueta: string; valor: number }[];
  alertas: { id: string; nivel: string; etiqueta: string; detalle: string; cantidad: number }[];
  revisar: ResidenteRevisar[];
  totalRegistros: number;
}

/**
 * Backend: DashboardRepository.relaciones (GET /dashboard/relaciones). Programa, servicio y
 * asignatura que aparecen juntos en alguna rotación; encadenan las opciones de los filtros.
 */
export interface RelacionFiltros {
  programa: string;
  servicio: string;
  asignaturaId: number;
}

/**
 * Residentes del periodo de un programa. Un residente es del periodo si tiene alguna rotación
 * (no cancelada) que inicia en él.
 */
export interface SituacionPrograma {
  etiqueta: string;
  /** Con al menos una rotación asignada. */
  conRotacion: number;
  /** Sin rotación asignada: todas las del periodo esperan cupo. */
  enEspera: number;
}

/** Pico mensual de residentes simultáneos por servicio frente a su cupo. */
export interface MapaOcupacion {
  meses: string[];
  filas: {
    servicio: string;
    cupo: number;
    /** Días de la ventana en que el servicio superó su cupo simultáneo. */
    diasExceso: number;
    /** Mayor porcentaje de la fila; ordena los servicios más presionados primero. */
    maximo: number;
    celdas: { mes: string; pico: number; porcentaje: number }[];
  }[];
}

export type MotivoRevision = 'cruce' | 'espera';

/** Residente que el administrador debe revisar y por qué. */
export interface ResidenteRevisar {
  residenteId: number;
  residente: string;
  programa: string;
  motivo: MotivoRevision;
  detalle: string;
}
