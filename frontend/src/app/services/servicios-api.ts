import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from './consulta';
export interface Servicio {
  id: number;
  nombre: string;
  /** Opcional en la base de datos: puede llegar sin diligenciar. */
  descripcion: string | null;
  cupoSimultaneo: number;
  cupoMaximoInstalado: number;
  activo: boolean;
}
@Injectable({providedIn: 'root'})
export class ServiciosApi {
 private readonly http = inject(HttpClient);
 listar(params: Parametros) { return this.http.get<Pagina<Servicio>>('/api/servicios', {params}); }
}
