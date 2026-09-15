import { API_URL } from './api.config';
import { DashboardDatos } from '../models/dashboard.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Parametros } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  consultar(params: Parametros) {
    return this.http.get<DashboardDatos>(`${API_URL}/dashboard`, { params });
  }
}
