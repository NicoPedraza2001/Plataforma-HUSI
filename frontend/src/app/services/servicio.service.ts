import { API_URL } from './api.config';
import { Servicio } from '../models/servicio.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class ServicioService {
  private readonly http = inject(HttpClient);
  listar(params: Parametros) {
    return this.http.get<Pagina<Servicio>>(`${API_URL}/servicios`, { params });
  }
}
