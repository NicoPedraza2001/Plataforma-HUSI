import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { Pagina, Parametros } from './consulta';
import { UniversidadCatalogo, UniversidadDetalle, UniversidadDatos } from '../models/universidad.model';
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
  buscar(params: Parametros = {}) { return this.http.get<Pagina<UniversidadDetalle>>('/api/universidades/buscar', { params }); }
  catalogo() { return this.http.get<UniversidadCatalogo[]>('/api/universidades'); }
  consultar(id: number) { return this.http.get<UniversidadDetalle>('/api/universidades/' + id); }
  crear(datos: UniversidadDatos) { return this.http.post<UniversidadDetalle>('/api/universidades', datos); }
  actualizar(id: number, datos: UniversidadDatos) { return this.http.put<UniversidadDetalle>('/api/universidades/' + id, datos); }
  cambiarEstado(id: number, estado: string) { return this.http.patch<UniversidadDetalle>('/api/universidades/' + id + '/estado', {estado}); }
  eliminarDefinitivamente(id: number) { return this.http.delete<void>('/api/universidades/' + id + '/definitivo'); }
  desactivar(id: number) { return this.http.delete<void>('/api/universidades/' + id); }
  activar(id: number) { return this.http.patch<UniversidadDetalle>('/api/universidades/' + id + '/estado', { estado: 'Activo' }); }
}
