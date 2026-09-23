import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { Residente, ResidenteRequest, ResidenteDetalle } from '../models/residente.model';
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

  /** GET /api/residentes/{id} */
  consultar(id: number): Observable<ResidenteDetalle> {
    return this.http.get<ResidenteDetalle>(`${API_URL}/residentes/${id}`);
  }
  /** POST /api/residentes */
  crear(datos: ResidenteRequest): Observable<ResidenteDetalle> {
    return this.http.post<ResidenteDetalle>(`${API_URL}/residentes`, datos);
  }
  /** PUT /api/residentes/{id} */
  actualizar(id: number, datos: ResidenteRequest): Observable<ResidenteDetalle> {
    return this.http.put<ResidenteDetalle>(`${API_URL}/residentes/${id}`, datos);
  }
  /** PATCH /api/residentes/{id}/estado */
  cambiarEstado(id: number, estado: string): Observable<ResidenteDetalle> {
    return this.http.patch<ResidenteDetalle>(`${API_URL}/residentes/${id}/estado`, { estado });
  }
  /** DELETE /api/residentes/{id}/definitivo */
  eliminarDefinitivamente(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/residentes/${id}/definitivo`);
  }
}
