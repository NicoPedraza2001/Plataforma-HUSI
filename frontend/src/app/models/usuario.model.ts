/** Backend: fila del listado GET /usuarios (repository/query/ListadoSql, «usuarios»). */
export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: string;
  /** Servicio que coordina; `null` si el usuario no es coordinador. */
  servicio: string | null;
  activo: boolean;
}

/**
 * Backend: dto/request/UsuarioRequest. La respuesta de usuario nunca incluye contraseña.
 * `servicioId` es obligatorio con el rol Coordinador y el backend lo ignora con otro rol.
 */
export interface UsuarioRequest {
  nombre: string;
  apellido: string;
  correo: string;
  contrasena: string;
  rolId: number | null;
  servicioId: number | null;
}

/** Backend: dto/response/UsuarioResponse. */
export interface UsuarioDetalle extends Omit<UsuarioRequest, 'contrasena'> {
  id: number;
  estado: string;
  servicio: string | null;
}
