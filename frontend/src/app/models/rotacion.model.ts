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
