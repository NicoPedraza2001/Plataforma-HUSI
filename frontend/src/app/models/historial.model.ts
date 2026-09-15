/** Contratos de respuesta y solicitudes del dominio; conserva los campos de la API. */
export interface RegistroHistorial {
  id: number;
  fecha: string;
  usuario: string;
  modulo: string;
  accion: string;
  registro: string;
  detalle: string;
}
