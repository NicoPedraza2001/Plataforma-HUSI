/** Contratos de respuesta y solicitudes del dominio; conserva los campos de la API. */
export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: string;
  activo: boolean;
}

/** Datos editables; la respuesta de usuario nunca incluye contraseña. */
export interface UsuarioRequest {
  nombre: string;
  apellido: string;
  correo: string;
  contrasena: string;
  rolId: number | null;
}
export interface UsuarioDetalle extends Omit<UsuarioRequest, 'contrasena'> {
  id: number;
  estado: string;
}
