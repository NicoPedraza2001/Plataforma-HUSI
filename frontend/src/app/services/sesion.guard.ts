import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Perfil } from '../models/sesion.model';
import { SesionActual } from './sesion-actual';

/** Parámetro de `/login` con la ruta a la que se vuelve después de iniciar sesión. */
export const PARAMETRO_VOLVER = 'volver';

/**
 * Deja entrar a la ruta solo si hay sesión con el rol indicado; si no, lleva al login.
 * Es una ayuda de navegación: aunque alguien la saltara, el backend rechaza cada petición
 * sin sesión (401) o sin el rol (403).
 */
export function requiereRol(rol: string): CanActivateFn {
  return async (_ruta, estado) => {
    const router = inject(Router);
    let perfil: Perfil | null;
    try {
      perfil = await inject(SesionActual).consultar();
    } catch {
      // Backend sin respuesta: el login mostrará el error al intentar entrar.
      perfil = null;
    }
    if (perfil?.rol === rol) return true;
    return router.createUrlTree(['/login'], {
      queryParams: perfil ? {} : { [PARAMETRO_VOLVER]: estado.url },
    });
  };
}
