import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ResidentesACargo } from '../models/coordinador.model';
import { API_URL } from './api.config';

/**
 * Backend: controller/CoordinadorController. El servicio coordinado sale de la sesión: no se
 * envía en la petición.
 */
@Injectable({ providedIn: 'root' })
export class CoordinadorService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/coordinador`;

  /** GET /coordinador/residentes */
  listarResidentes(): Observable<ResidentesACargo> {
    return this.http.get<ResidentesACargo>(`${this.url}/residentes`);
  }
}
