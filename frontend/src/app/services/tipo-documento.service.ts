import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TipoDocumento } from '../models/tipo-documento.model';
import { API_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class TipoDocumentoService {
  private readonly http = inject(HttpClient);

  /** GET /tipos-documento */
  listar(): Observable<TipoDocumento[]> {
    return this.http.get<TipoDocumento[]>(`${API_URL}/tipos-documento`);
  }
}
