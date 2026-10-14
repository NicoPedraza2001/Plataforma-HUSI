import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { API_URL } from './api.config';
import { SesionActual } from './sesion-actual';
import { PARAMETRO_VOLVER } from './sesion.guard';

/** Parámetro de `/login` que indica por qué se llegó ahí. */
export const PARAMETRO_MOTIVO = 'motivo';
export const MOTIVO_SESION_EXPIRADA = 'expirada';

/**
 * Si el backend responde 401 en medio del trabajo (la sesión expiró por inactividad o se cerró
 * en otra pestaña), olvida la sesión y lleva al login para volver a la misma pantalla.
 * Las rutas de `/sesion` se excluyen: su 401 lo maneja quien las llama (login, guard).
 */
export const sesionExpiradaInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const sesionActual = inject(SesionActual);
  const router = inject(Router);
  const esRutaDeSesion = peticion.url.startsWith(`${API_URL}/sesion/`);

  return siguiente(peticion).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !esRutaDeSesion) {
        sesionActual.olvidar();
        router.navigate(['/login'], {
          queryParams: {
            [PARAMETRO_VOLVER]: router.url,
            [PARAMETRO_MOTIVO]: MOTIVO_SESION_EXPIRADA,
          },
        });
      }
      return throwError(() => error);
    }),
  );
};
