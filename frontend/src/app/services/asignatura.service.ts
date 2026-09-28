import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Asignatura, AsignaturaRequest } from '../models/asignatura.model';
import { Pagina, Parametros } from '../models/pagina.model';
import { API_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class AsignaturaService {
  private readonly http = inject(HttpClient);

  /** GET /api/asignaturas */
  listar(params: Parametros): Observable<Pagina<Asignatura>> {
    return this.http.get<Pagina<Asignatura>>(`${API_URL}/asignaturas`, { params });
  }

  /** GET /api/asignaturas/{id} */
  consultar(id: number): Observable<Asignatura> {
    return this.http.get<Asignatura>(`${API_URL}/asignaturas/${id}`);
  }

  /** POST /api/asignaturas */
  crear(datos: AsignaturaRequest): Observable<Asignatura> {
    return this.http.post<Asignatura>(`${API_URL}/asignaturas`, datos);
  }

  /** PUT /api/asignaturas/{id} */
  actualizar(id: number, datos: AsignaturaRequest): Observable<Asignatura> {
    return this.http.put<Asignatura>(`${API_URL}/asignaturas/${id}`, datos);
  }

  /** PATCH /api/asignaturas/{id}/estado */
  cambiarEstado(id: number, estado: string): Observable<Asignatura> {
    return this.http.patch<Asignatura>(`${API_URL}/asignaturas/${id}/estado`, { estado });
  }

  /** DELETE /api/asignaturas/{id}/definitivo */
  eliminarDefinitivamente(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/asignaturas/${id}/definitivo`);
  }
}
