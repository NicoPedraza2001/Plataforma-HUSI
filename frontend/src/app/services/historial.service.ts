import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { RegistroHistorial } from '../models/historial.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class HistorialService {
  private readonly http = inject(HttpClient);
  /** GET /api/historial */
  listar(params: Parametros): Observable<Pagina<RegistroHistorial>> {
    return this.http.get<Pagina<RegistroHistorial>>(`${API_URL}/historial`, { params });
  }
}
