import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { Rotacion } from '../models/rotacion.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from '../models/pagina.model';
/** Consulta histórica de solo lectura utilizada por el dashboard. El CRUD se retiró. */
@Injectable({ providedIn: 'root' })
export class RotacionService {
  private readonly http = inject(HttpClient);
  listar(params: Parametros): Observable<Pagina<Rotacion>> {
    return this.http.get<Pagina<Rotacion>>(`${API_URL}/rotaciones`, { params });
  }
}
