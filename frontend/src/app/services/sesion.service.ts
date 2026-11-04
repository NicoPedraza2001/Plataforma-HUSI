import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ActualizacionCredencialesRequest,
  ActualizacionPerfilRequest,
  InicioSesionRequest,
  Perfil,
} from '../models/sesion.model';
import { API_URL } from './api.config';

/**
 * Backend: controller/SesionController. La sesión vive en el servidor: el navegador solo guarda
 * la cookie HttpOnly `HUSI_SESION`, que este código no puede leer ni necesita enviar a mano.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly http = inject(HttpClient);

  /** POST /api/sesion/iniciar */
  iniciarSesion(credenciales: InicioSesionRequest): Observable<Perfil> {
    return this.http.post<Perfil>(`${API_URL}/sesion/iniciar`, credenciales);
  }

  /** POST /api/sesion/cerrar */
  cerrarSesion(): Observable<void> {
    return this.http.post<void>(`${API_URL}/sesion/cerrar`, null);
  }

  /** GET /api/sesion/perfil */
  perfil(): Observable<Perfil> {
    return this.http.get<Perfil>(`${API_URL}/sesion/perfil`);
  }

  /** PATCH /api/sesion/perfil */
  actualizarPerfil(request: ActualizacionPerfilRequest): Observable<Perfil> {
    return this.http.patch<Perfil>(`${API_URL}/sesion/perfil`, request);
  }

  /** PATCH /api/sesion/credenciales */
  actualizarCredenciales(request: ActualizacionCredencialesRequest): Observable<void> {
    return this.http.patch<void>(`${API_URL}/sesion/credenciales`, request);
  }
}
