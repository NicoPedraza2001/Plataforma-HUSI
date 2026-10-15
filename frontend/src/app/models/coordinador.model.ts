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
 * Backend: dto/response/ResidentesACargoResponse. Solo trae las rotaciones en curso a `fecha`
 * (hoy en Bogotá); las finalizadas y las futuras no llegan.
 */
export interface ResidentesACargo {
  servicio: string;
  fecha: string;
  residentes: ResidenteACargo[];
}
