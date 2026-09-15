/** Contratos de respuesta y solicitudes del dominio; conserva los campos de la API. */
export interface DashboardDatos {
  indicadores: {
    id: string;
    etiqueta: string;
    valor: string;
    detalle: string;
    delta?: string;
    tono: string;
  }[];
  rotacionesPorServicio: { etiqueta: string; valor: number; maximo: number }[];
  ocupacionPorServicio: { etiqueta: string; valor: number; maximo: number }[];
  distribucionUniversidad: { etiqueta: string; porcentaje: number }[];
  tendenciaInicios: { etiqueta: string; valor: number }[];
  alertas: { id: string; nivel: string; etiqueta: string; detalle: string; cantidad: number }[];
  totalRegistros: number;
}
