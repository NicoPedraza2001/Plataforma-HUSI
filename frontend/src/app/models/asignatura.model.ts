/** Backend: dto/response/AsignaturaResponse. */
export interface Asignatura {
  id: number;
  nombre: string;
  servicioId: number;
  servicio: string;
  estado: string;
  activo: boolean;
}
/** Backend: dto/request/AsignaturaRequest. */
export interface AsignaturaRequest {
  nombre: string;
  servicioId: number | null;
}
