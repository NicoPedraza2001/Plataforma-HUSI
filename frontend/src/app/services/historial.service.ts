import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RegistroHistorial } from '../models/historial.model';
import { Pagina, Parametros } from '../models/pagina.model';
import { API_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class HistorialService {
  private readonly http = inject(HttpClient);

  /** GET /api/historial */
  listar(params: Parametros): Observable<Pagina<RegistroHistorial>> {
    return this.http.get<Pagina<RegistroHistorial>>(`${API_URL}/historial`, { params });
  }
}
