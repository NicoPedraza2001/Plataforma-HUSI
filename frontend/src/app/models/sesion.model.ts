/** Backend: model/enums/RolUsuario (valor de `rol.nombre_rol`). */
export const ROL_ADMINISTRADOR = 'Administrador';
export const ROL_COORDINADOR = 'Coordinador';

/** Backend: dto/request/InicioSesionRequest. */
export interface InicioSesionRequest {
  correo: string;
  contrasena: string;
}

/** Backend: dto/response/PerfilResponse. Nunca incluye la contraseña. */
export interface Perfil {
  nombres: string;
  apellidos: string;
  correo: string;
  rol: string;
}
