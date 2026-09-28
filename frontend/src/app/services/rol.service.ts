import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { OpcionCatalogo } from '../models/opcion-catalogo.model';
import { API_URL } from './api.config';
@Injectable({ providedIn: 'root' })
export class RolService {
  private readonly http = inject(HttpClient);

  /** GET /api/roles */
  listar(): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(`${API_URL}/roles`);
  }
}
