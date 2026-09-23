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

/** Datos editables; la respuesta de usuario nunca incluye contraseña. */
export interface ResidenteRequest {
  nombre: string;
  tipoDocumento: string;
  numeroDocumento: string;
  perfilAcademico: string;
  programaId: number | null;
}
export interface ResidenteDetalle extends Omit<ResidenteRequest, 'contrasena'> {
  id: number;
  estado: string;
}
