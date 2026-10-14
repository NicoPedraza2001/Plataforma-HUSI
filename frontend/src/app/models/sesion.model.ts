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

/**
 * Backend: dto/request/ActualizacionPerfilRequest. Solo se envían los campos que cambian; los
 * que se omiten se conservan.
 */
export interface ActualizacionPerfilRequest {
  nombres?: string;
  apellidos?: string;
}

/**
 * Backend: dto/request/ActualizacionCredencialesRequest. La contraseña actual siempre es
 * obligatoria; el correo y la contraseña nueva solo se envían si cambian.
 */
export interface ActualizacionCredencialesRequest {
  contrasenaActual: string;
  correo?: string;
  contrasenaNueva?: string;
}
