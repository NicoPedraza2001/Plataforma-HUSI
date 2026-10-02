import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { OpcionCatalogo } from '../models/opcion-catalogo.model';
import { API_URL } from './api.config';

/** Backend: controller/PerfilAcademicoController; responde dto/response/CatalogoResponse. */
@Injectable({ providedIn: 'root' })
export class PerfilAcademicoService {
  private readonly http = inject(HttpClient);

  /** GET /perfiles-academicos */
  listar(): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(`${API_URL}/perfiles-academicos`);
  }
}
