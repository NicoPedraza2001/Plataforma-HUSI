/** Backend: dto/response/CoordinadorResponse. */
export interface Coordinador {
  id: number;
  nombre: string;
  servicioId: number;
  servicio: string;
  estado: string;
  activo: boolean;
  usuarioId: number;
  correo: string;
}
/** Backend: dto/request/CoordinadorRequest. */
export interface CoordinadorRequest {
  usuarioId: number | null;
  servicioId: number | null;
}
