import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { OpcionCatalogo } from '../models/opcion-catalogo.model';
import { Pagina, Parametros } from '../models/pagina.model';
import {
  CatalogosRotacion,
  FilaRotacion,
  OrigenEspera,
  ResidenteEnEspera,
  ResidenteRotacion,
  ResultadoRotaciones,
  Rotacion,
  RotacionDatos,
  RotacionGestion,
} from '../models/rotacion.model';
import { API_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class RotacionService {
  // Dependencias
  private readonly http = inject(HttpClient);

  private readonly urlGestion = `${API_URL}/rotaciones/gestion`;

  /** GET /api/rotaciones/gestion */
  listarGestion(params: Parametros): Observable<Pagina<RotacionGestion>> {
    return this.http.get<Pagina<RotacionGestion>>(this.urlGestion, { params });
  }

  /** GET /api/rotaciones/gestion/catalogos */
  catalogos(): Observable<CatalogosRotacion> {
    return this.http.get<CatalogosRotacion>(this.urlGestion + '/catalogos');
  }

  /** GET /api/rotaciones/gestion/{asignatura}/{inicio}/{fin} */
  consultar(rotacion: RotacionDatos): Observable<RotacionGestion> {
    return this.http.get<RotacionGestion>(this.urlDeRotacion(rotacion));
  }

  /**
   * GET /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/residentes
   * Con `soloEnEspera` trae solo «Esperando cupo» por orden de llegada; sin él, el resto por
   * nombre.
   */
  listarResidentes(
    rotacion: RotacionDatos,
    pagina = 1,
    soloEnEspera = false,
  ): Observable<Pagina<ResidenteRotacion>> {
    return this.http.get<Pagina<ResidenteRotacion>>(this.urlDeRotacion(rotacion) + '/residentes', {
      params: { pagina, espera: soloEnEspera },
    });
  }

  /** GET /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/candidatos */
  candidatos(rotacion: RotacionDatos, busqueda: string): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(this.urlDeRotacion(rotacion) + '/candidatos', {
      params: { q: busqueda },
    });
  }

  /** POST /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/residentes */
  asignar(rotacion: RotacionDatos, residenteId: number): Observable<void> {
    return this.http.post<void>(this.urlDeRotacion(rotacion) + '/residentes', { residenteId });
  }

  /** PATCH /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/residentes/{residente}/estado */
  cambiarEstadoResidente(
    rotacion: RotacionDatos,
    residenteId: number,
    estado: string,
  ): Observable<void> {
    return this.http.patch<void>(
      this.urlDeRotacion(rotacion) + '/residentes/' + residenteId + '/estado',
      { estado },
    );
  }

  /** DELETE /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/residentes/{residente} */
  eliminarResidente(rotacion: RotacionDatos, residenteId: number): Observable<void> {
    return this.http.delete<void>(this.urlDeRotacion(rotacion) + '/residentes/' + residenteId);
  }

  /**
   * GET /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/espera
   * Residentes en espera en otras rotaciones del mismo servicio.
   */
  listarEsperaDelServicio(rotacion: RotacionDatos): Observable<ResidenteEnEspera[]> {
    return this.http.get<ResidenteEnEspera[]>(this.urlDeRotacion(rotacion) + '/espera');
  }

  /** POST /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/espera */
  trasladarEspera(rotacion: RotacionDatos, residentes: OrigenEspera[]): Observable<void> {
    return this.http.post<void>(this.urlDeRotacion(rotacion) + '/espera', { residentes });
  }

  /** POST /api/rotaciones/gestion */
  crear(datos: RotacionDatos): Observable<RotacionGestion> {
    return this.http.post<RotacionGestion>(this.urlGestion, datos);
  }

  /** PUT /api/rotaciones/gestion/{asignatura}/{inicio}/{fin} */
  editar(rotacion: RotacionDatos, datos: RotacionDatos): Observable<RotacionGestion> {
    return this.http.put<RotacionGestion>(this.urlDeRotacion(rotacion), datos);
  }

  /** PATCH /api/rotaciones/gestion/{asignatura}/{inicio}/{fin}/estado */
  cambiarEstado(rotacion: RotacionDatos, activa: boolean): Observable<RotacionGestion> {
    return this.http.patch<RotacionGestion>(this.urlDeRotacion(rotacion) + '/estado', { activa });
  }

  /** DELETE /api/rotaciones/gestion/{asignatura}/{inicio}/{fin} */
  eliminar(rotacion: RotacionDatos): Observable<void> {
    return this.http.delete<void>(this.urlDeRotacion(rotacion));
  }

  /** POST /api/rotaciones/gestion/importar/validar */
  validar(filas: FilaRotacion[]): Observable<ResultadoRotaciones> {
    return this.http.post<ResultadoRotaciones>(this.urlGestion + '/importar/validar', { filas });
  }

  /** POST /api/rotaciones/gestion/importar */
  importar(filas: FilaRotacion[]): Observable<ResultadoRotaciones> {
    return this.http.post<ResultadoRotaciones>(this.urlGestion + '/importar', { filas });
  }

  /** GET /api/rotaciones */
  listar(params: Parametros): Observable<Pagina<Rotacion>> {
    return this.http.get<Pagina<Rotacion>>(`${API_URL}/rotaciones`, { params });
  }

  /** La rotación se identifica por su clave compuesta: asignatura, inicio y fin. */
  private urlDeRotacion(rotacion: RotacionDatos) {
    return `${this.urlGestion}/${rotacion.asignaturaId}/${rotacion.inicio}/${rotacion.fin}`;
  }
}
