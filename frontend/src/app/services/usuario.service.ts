import { API_URL } from './api.config';
import { Usuario } from '../models/usuario.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);
  listar(params: Parametros) {
    return this.http.get<Pagina<Usuario>>(`${API_URL}/usuarios`, { params });
  }
}
