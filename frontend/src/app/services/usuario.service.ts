import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { Usuario, UsuarioRequest, UsuarioDetalle } from '../models/usuario.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);
  /** GET /api/usuarios */
  listar(params: Parametros): Observable<Pagina<Usuario>> {
    return this.http.get<Pagina<Usuario>>(`${API_URL}/usuarios`, { params });
  }

  /** GET /api/usuarios/{id} */
  consultar(id: number): Observable<UsuarioDetalle> {
    return this.http.get<UsuarioDetalle>(`${API_URL}/usuarios/${id}`);
  }
  /** POST /api/usuarios */
  crear(datos: UsuarioRequest): Observable<UsuarioDetalle> {
    return this.http.post<UsuarioDetalle>(`${API_URL}/usuarios`, datos);
  }
  /** PUT /api/usuarios/{id} */
  actualizar(id: number, datos: UsuarioRequest): Observable<UsuarioDetalle> {
    return this.http.put<UsuarioDetalle>(`${API_URL}/usuarios/${id}`, datos);
  }
  /** PATCH /api/usuarios/{id}/estado */
  cambiarEstado(id: number, estado: string): Observable<UsuarioDetalle> {
    return this.http.patch<UsuarioDetalle>(`${API_URL}/usuarios/${id}/estado`, { estado });
  }
  /** DELETE /api/usuarios/{id}/definitivo */
  eliminarDefinitivamente(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/usuarios/${id}/definitivo`);
  }
}
