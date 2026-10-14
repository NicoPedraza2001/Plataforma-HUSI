import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { loteImportacionInterceptor } from './services/lote-importacion.interceptor';
import { sesionExpiradaInterceptor } from './services/sesion-expirada.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // HttpClient copia por sí solo el token CSRF (cookie XSRF-TOKEN → cabecera X-XSRF-TOKEN) en
    // las peticiones que modifican datos; el backend lo exige junto con la cookie de sesión.
    provideHttpClient(withInterceptors([loteImportacionInterceptor, sesionExpiradaInterceptor])),
  ],
};
