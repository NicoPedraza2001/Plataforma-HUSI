import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from './consulta';
export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: string;
  activo: boolean;
}
@Injectable({providedIn: 'root'})
export class UsuariosApi {
 private readonly http = inject(HttpClient);
 listar(params: Parametros) { return this.http.get<Pagina<Usuario>>('/api/usuarios', {params}); }
}
