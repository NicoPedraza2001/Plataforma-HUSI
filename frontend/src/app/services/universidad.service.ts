import { Programa } from '../models/programa.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { Pagina, Parametros } from '../models/pagina.model';
import {
  UniversidadCatalogo,
  UniversidadDetalle,
  UniversidadDatos,
} from '../models/universidad.model';
import { Universidad } from '../models/universidad.model';

/**
 * Cliente HTTP del catálogo de universidades.
 * Backend: `controller/UniversidadController` (`/api/universidades`).
 */
@Injectable({ providedIn: 'root' })
export class UniversidadService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/universidades`;

  /** GET /universidades — solo universidades activas. */
  listar(): Observable<Universidad[]> {
    return this.http.get<Universidad[]>(this.url);
  }
  /** GET /api/universidades/{id}/programas */
  programas(id: number, params: Parametros): Observable<Pagina<Programa>> {
    return this.http.get<Pagina<Programa>>(`${this.url}/${id}/programas`, { params });
  }
  /** GET /api/universidades/buscar */
  buscar(params: Parametros = {}): Observable<Pagina<UniversidadDetalle>> {
    return this.http.get<Pagina<UniversidadDetalle>>(`${API_URL}/universidades/buscar`, { params });
  }
  /** GET /api/universidades/{id} */
  consultar(id: number): Observable<UniversidadDetalle> {
    return this.http.get<UniversidadDetalle>(`${API_URL}/universidades/` + id);
  }
  /** POST /api/universidades */
  crear(datos: UniversidadDatos): Observable<UniversidadDetalle> {
    return this.http.post<UniversidadDetalle>(`${API_URL}/universidades`, datos);
  }
  /** PUT /api/universidades/{id} */
  actualizar(id: number, datos: UniversidadDatos): Observable<UniversidadDetalle> {
    return this.http.put<UniversidadDetalle>(`${API_URL}/universidades/` + id, datos);
  }
  /** PATCH /api/universidades/{id}/estado */
  cambiarEstado(id: number, estado: string): Observable<UniversidadDetalle> {
    return this.http.patch<UniversidadDetalle>(`${API_URL}/universidades/` + id + '/estado', {
      estado,
    });
  }
  /** DELETE /api/universidades/{id}/definitivo */
  eliminarDefinitivamente(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/universidades/` + id + '/definitivo');
  }
  /** DELETE /api/universidades/{id} */
  desactivar(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/universidades/` + id);
  }
}
