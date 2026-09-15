import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from './consulta';
export interface Residente {
  id: number;
  nombre: string;
  documento: string;
  universidad: string;
  programa: string;
  estado: string;
  periodo: string;
}
@Injectable({providedIn: 'root'})
export class ResidentesApi {
 private readonly http = inject(HttpClient);
 listar(params: Parametros) { return this.http.get<Pagina<Residente>>('/api/residentes', {params}); }
}
