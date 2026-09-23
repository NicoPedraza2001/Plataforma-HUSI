import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { Servicio, ServicioRequest } from '../models/servicio.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class ServicioService {
  private readonly http = inject(HttpClient);
  /** GET /api/servicios */
  listar(params: Parametros): Observable<Pagina<Servicio>> {
    return this.http.get<Pagina<Servicio>>(`${API_URL}/servicios`, { params });
  }

  /** GET /api/servicios/{id} */
  consultar(id: number): Observable<Servicio> {
    return this.http.get<Servicio>(`${API_URL}/servicios/${id}`);
  }
  /** POST /api/servicios */
  crear(datos: ServicioRequest): Observable<Servicio> {
    return this.http.post<Servicio>(`${API_URL}/servicios`, datos);
  }
  /** PUT /api/servicios/{id} */
  actualizar(id: number, datos: ServicioRequest): Observable<Servicio> {
    return this.http.put<Servicio>(`${API_URL}/servicios/${id}`, datos);
  }
  /** PATCH /api/servicios/{id}/estado */
  cambiarEstado(id: number, estado: string): Observable<Servicio> {
    return this.http.patch<Servicio>(`${API_URL}/servicios/${id}/estado`, { estado });
  }
  /** DELETE /api/servicios/{id}/definitivo */
  eliminarDefinitivamente(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/servicios/${id}/definitivo`);
  }
}
