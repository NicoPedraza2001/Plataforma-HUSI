/** Backend: dto/response/AsignaturaResponse. */
export interface Asignatura {
  id: number;
  nombre: string;
  servicioId: number;
  servicio: string;
  estado: string;
  activo: boolean;
}
/** Backend: dto/response/AsignaturaCatalogoResponse. El nombre se repite entre servicios. */
export interface AsignaturaCatalogo {
  id: number;
  nombre: string;
  servicio: string;
}
/** Backend: dto/request/AsignaturaRequest. */
export interface AsignaturaRequest {
  nombre: string;
  servicioId: number | null;
}
