import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { OpcionCatalogo } from '../models/opcion-catalogo.model';
@Injectable({ providedIn: 'root' })
export class RolService {
  private readonly http = inject(HttpClient);
  /** GET /api/roles */
  listar(): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(`${API_URL}/roles`);
  }
}
