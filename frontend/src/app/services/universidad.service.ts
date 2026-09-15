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
  programas(id: number, params: Parametros) {
    return this.http.get<Pagina<Programa>>(`${this.url}/${id}/programas`, { params });
  }
  buscar(params: Parametros = {}) {
    return this.http.get<Pagina<UniversidadDetalle>>(`${API_URL}/universidades/buscar`, { params });
  }
  consultar(id: number) {
    return this.http.get<UniversidadDetalle>(`${API_URL}/universidades/` + id);
  }
  crear(datos: UniversidadDatos) {
    return this.http.post<UniversidadDetalle>(`${API_URL}/universidades`, datos);
  }
  actualizar(id: number, datos: UniversidadDatos) {
    return this.http.put<UniversidadDetalle>(`${API_URL}/universidades/` + id, datos);
  }
  cambiarEstado(id: number, estado: string) {
    return this.http.patch<UniversidadDetalle>(`${API_URL}/universidades/` + id + '/estado', {
      estado,
    });
  }
  eliminarDefinitivamente(id: number) {
    return this.http.delete<void>(`${API_URL}/universidades/` + id + '/definitivo');
  }
  desactivar(id: number) {
    return this.http.delete<void>(`${API_URL}/universidades/` + id);
  }
}
