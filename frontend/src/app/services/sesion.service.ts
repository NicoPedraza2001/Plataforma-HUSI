import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Perfil } from '../models/sesion.model';
import { API_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly http = inject(HttpClient);

  /** GET /api/sesion/perfil */
  perfil(): Observable<Perfil> {
    return this.http.get<Perfil>(`${API_URL}/sesion/perfil`);
  }
}
