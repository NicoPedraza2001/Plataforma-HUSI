import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Catalogos } from '../models/catalogos.model';
import { API_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);

  /** GET /api/catalogos */
  listar(): Observable<Catalogos> {
    return this.http.get<Catalogos>(`${API_URL}/catalogos`);
  }
}
