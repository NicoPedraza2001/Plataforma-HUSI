import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from './consulta';
export interface RegistroHistorial {
  id: number;
  fecha: string;
  usuario: string;
  modulo: string;
  accion: string;
  registro: string;
  detalle: string;
}
@Injectable({providedIn: 'root'})
export class HistorialApi {
 private readonly http = inject(HttpClient);
 listar(params: Parametros) { return this.http.get<Pagina<RegistroHistorial>>('/api/historial', {params}); }
}
