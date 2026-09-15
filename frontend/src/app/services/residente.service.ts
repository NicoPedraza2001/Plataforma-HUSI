import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { Residente } from '../models/residente.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class ResidenteService {
  private readonly http = inject(HttpClient);
  /** GET /api/residentes */
  listar(params: Parametros): Observable<Pagina<Residente>> {
    return this.http.get<Pagina<Residente>>(`${API_URL}/residentes`, { params });
  }
}
