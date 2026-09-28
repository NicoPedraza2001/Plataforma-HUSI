import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { FilaImportacion, ResultadoImportacion } from '../models/importacion-programas.model';
import { Pagina, Parametros } from '../models/pagina.model';
import { Programa, ProgramaRequest } from '../models/programa.model';
import { API_URL } from './api.config';

/**
 * Cliente HTTP de programas académicos.
 * Backend: `controller/ProgramaController` (`/api/programas`). Los métodos llevan el mismo
 * nombre que en el controller para encontrarlos fácilmente en ambos lados.
 */
@Injectable({ providedIn: 'root' })
export class ProgramaService {
  // Dependencias
  private readonly http = inject(HttpClient);

  private readonly url = `${API_URL}/programas`;

  /** GET /api/programas/buscar */
  buscar(params: Parametros = {}): Observable<Pagina<Programa>> {
    return this.http.get<Pagina<Programa>>(`${this.url}/buscar`, { params });
  }

  /** PATCH /api/programas/{id}/estado */
  cambiarEstado(id: number, estado: string): Observable<Programa> {
    return this.http.patch<Programa>(`${this.url}/${id}/estado`, { estado });
  }

  /** DELETE /api/programas/{id}/definitivo */
  eliminarDefinitivamente(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}/definitivo`);
  }

  /** GET /programas — solo programas activos. */
  listar(): Observable<Programa[]> {
    return this.http.get<Programa[]>(this.url);
  }

  /** GET /programas/{id} — incluye programas inactivos. */
  consultar(id: number): Observable<Programa> {
    return this.http.get<Programa>(`${this.url}/${id}`);
  }

  /** POST /programas */
  crear(datos: ProgramaRequest): Observable<Programa> {
    return this.http.post<Programa>(this.url, datos);
  }

  /** PUT /programas/{id} */
  actualizar(id: number, datos: ProgramaRequest): Observable<Programa> {
    return this.http.put<Programa>(`${this.url}/${id}`, datos);
  }

  /** DELETE /programas/{id} — borrado lógico: pasa el programa a Inactivo. */
  desactivar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** POST /programas/importacion/validar — revisa las filas sin guardar nada. */
  validar(filas: FilaImportacion[]): Observable<ResultadoImportacion> {
    return this.http.post<ResultadoImportacion>(`${this.url}/importacion/validar`, { filas });
  }

  /** POST /programas/importacion — guarda las filas válidas y devuelve el resultado por fila. */
  importar(filas: FilaImportacion[]): Observable<ResultadoImportacion> {
    return this.http.post<ResultadoImportacion>(`${this.url}/importacion`, { filas });
  }
}
