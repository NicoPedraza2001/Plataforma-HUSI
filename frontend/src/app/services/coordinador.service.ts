import { OpcionCatalogo } from '../models/opcion-catalogo.model';
import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { Coordinador, CoordinadorRequest } from '../models/coordinador.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class CoordinadorService {
  private readonly http = inject(HttpClient);
  usuariosDisponibles(): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(`${API_URL}/coordinadores/usuarios-disponibles`);
  }
  /** GET /api/coordinadores */
  listar(params: Parametros): Observable<Pagina<Coordinador>> {
    return this.http.get<Pagina<Coordinador>>(`${API_URL}/coordinadores`, { params });
  }

  /** GET /api/coordinadores/{id} */
  consultar(id: number): Observable<Coordinador> {
    return this.http.get<Coordinador>(`${API_URL}/coordinadores/${id}`);
  }
  /** POST /api/coordinadores */
  crear(datos: CoordinadorRequest): Observable<Coordinador> {
    return this.http.post<Coordinador>(`${API_URL}/coordinadores`, datos);
  }
  /** PUT /api/coordinadores/{id} */
  actualizar(id: number, datos: CoordinadorRequest): Observable<Coordinador> {
    return this.http.put<Coordinador>(`${API_URL}/coordinadores/${id}`, datos);
  }
  /** PATCH /api/coordinadores/{id}/estado */
  cambiarEstado(id: number, estado: string): Observable<Coordinador> {
    return this.http.patch<Coordinador>(`${API_URL}/coordinadores/${id}/estado`, { estado });
  }
  /** DELETE /api/coordinadores/{id}/definitivo */
  eliminarDefinitivamente(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/coordinadores/${id}/definitivo`);
  }
}
