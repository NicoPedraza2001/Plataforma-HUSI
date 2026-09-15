/** Contratos de respuesta y solicitudes del dominio; conserva los campos de la API. */
export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: string;
  activo: boolean;
}
