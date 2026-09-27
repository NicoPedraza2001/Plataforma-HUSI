import { OpcionCatalogo } from '../models/opcion-catalogo.model';
import { ResidenteRotacion } from '../models/rotacion.model';
import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import {
  Rotacion,
  RotacionDatos,
  RotacionGestion,
  CatalogosRotacion,
  FilaRotacion,
  ResultadoRotaciones,
} from '../models/rotacion.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class RotacionService {
  private readonly http = inject(HttpClient);
  private readonly gestion = `${API_URL}/rotaciones/gestion`;
  /** GET /api/rotaciones/gestion */
  gestionar(params: Parametros): Observable<Pagina<RotacionGestion>> {
    return this.http.get<Pagina<RotacionGestion>>(this.gestion, { params });
  }
  /** GET /api/rotaciones/gestion/catalogos */
  catalogos(): Observable<CatalogosRotacion> {
    return this.http.get<CatalogosRotacion>(this.gestion + '/catalogos');
  }
  /** GET /api/rotaciones/gestion/{asignatura}/{inicio}/{fin} */
  consultar(r: RotacionDatos): Observable<RotacionGestion> {
    return this.http.get<RotacionGestion>(this.ruta(r));
  }
  /** GET /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/residentes */
  residentes(r: RotacionDatos, pagina = 1): Observable<Pagina<ResidenteRotacion>> {
    return this.http.get<Pagina<ResidenteRotacion>>(this.ruta(r) + '/residentes', {
      params: { pagina },
    });
  }
  /** GET /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/candidatos */
  candidatos(r: RotacionDatos, q: string): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(this.ruta(r) + '/candidatos', { params: { q } });
  }
  /** POST /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/residentes */
  asignar(r: RotacionDatos, residenteId: number): Observable<void> {
    return this.http.post<void>(this.ruta(r) + '/residentes', { residenteId });
  }
  /** PATCH /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/residentes/{residente}/estado */
  estadoResidente(r: RotacionDatos, residente: number, estado: string): Observable<void> {
    return this.http.patch<void>(this.ruta(r) + '/residentes/' + residente + '/estado', { estado });
  }
  private ruta(r: RotacionDatos) {
    return `${this.gestion}/${r.asignaturaId}/${r.inicio}/${r.fin}`;
  }
  /** POST /api/rotaciones/gestion */
  crear(d: RotacionDatos): Observable<RotacionGestion> {
    return this.http.post<RotacionGestion>(this.gestion, d);
  }
  /** PUT /api/rotaciones/gestion/{asignatura}/{inicio}/{fin} */
  editar(r: RotacionDatos, d: RotacionDatos): Observable<RotacionGestion> {
    return this.http.put<RotacionGestion>(this.ruta(r), d);
  }
  /** PATCH /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/estado */
  estado(r: RotacionDatos, activa: boolean): Observable<RotacionGestion> {
    return this.http.patch<RotacionGestion>(this.ruta(r) + '/estado', { activa });
  }
  /** DELETE /api/rotaciones/gestion/{asignatura}/{inicio}/{fin} */
  eliminar(r: RotacionDatos): Observable<void> {
    return this.http.delete<void>(this.ruta(r));
  }
  /** POST /api/rotaciones/gestion/importar/validar */
  validar(filas: FilaRotacion[]): Observable<ResultadoRotaciones> {
    return this.http.post<ResultadoRotaciones>(this.gestion + '/importar/validar', { filas });
  }
  /** POST /api/rotaciones/gestion/importar */
  importar(filas: FilaRotacion[]): Observable<ResultadoRotaciones> {
    return this.http.post<ResultadoRotaciones>(this.gestion + '/importar', { filas });
  }
  /** GET /api/rotaciones */
  listar(params: Parametros): Observable<Pagina<Rotacion>> {
    return this.http.get<Pagina<Rotacion>>(`${API_URL}/rotaciones`, { params });
  }
}
