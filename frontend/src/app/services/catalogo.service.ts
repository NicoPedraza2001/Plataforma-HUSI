import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { Catalogos } from '../models/catalogos.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);
  /** GET /api/catalogos */
  listar(): Observable<Catalogos> {
    return this.http.get<Catalogos>(`${API_URL}/catalogos`);
  }
}
