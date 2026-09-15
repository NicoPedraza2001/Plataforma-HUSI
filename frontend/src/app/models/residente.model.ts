/** Contratos de respuesta y solicitudes del dominio; conserva los campos de la API. */
export interface Residente {
  id: number;
  nombre: string;
  documento: string;
  universidad: string;
  programa: string;
  estado: string;
  periodo: string;
}
