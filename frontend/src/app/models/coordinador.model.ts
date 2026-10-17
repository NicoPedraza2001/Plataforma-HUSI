/** Backend: dto/response/ResidenteACargoResponse. Fechas en ISO (`yyyy-MM-dd`). */
export interface ResidenteACargo {
  residenteId: number;
  nombre: string;
  documento: string;
  universidad: string;
  programa: string;
  asignatura: string;
  inicio: string;
  fin: string;
}

/**
 * Backend: dto/response/ResidentesACargoResponse. `residentes` son las rotaciones en curso a
 * `fecha` (hoy en Bogotá) y `proximos` las que empiezan después, ordenadas por fecha de inicio.
 * Las finalizadas no llegan.
 */
export interface ResidentesACargo {
  servicio: string;
  fecha: string;
  residentes: ResidenteACargo[];
  proximos: ResidenteACargo[];
}
