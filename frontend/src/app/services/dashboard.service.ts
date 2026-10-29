import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DashboardDatos, RelacionFiltros } from '../models/dashboard.model';
import { Parametros } from '../models/pagina.model';
import { API_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  /** GET /api/dashboard */
  consultar(params: Parametros): Observable<DashboardDatos> {
    return this.http.get<DashboardDatos>(`${API_URL}/dashboard`, { params });
  }

  /** GET /api/dashboard/relaciones */
  relaciones(): Observable<RelacionFiltros[]> {
    return this.http.get<RelacionFiltros[]>(`${API_URL}/dashboard/relaciones`);
  }

  /** GET /api/coordinador/dashboard: igual forma que /dashboard, acotado a su propio servicio. */
  consultarCoordinador(): Observable<DashboardDatos> {
    return this.http.get<DashboardDatos>(`${API_URL}/coordinador/dashboard`);
  }
}
