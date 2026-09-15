import { API_URL } from './api.config';
import {
  Rotacion,
  RotacionDatos,
  RotacionGestion,
  CatalogosRotacion,
  FilaRotacion,
  ResultadoRotaciones,
} from '../models/rotacion.model';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class RotacionService {
  private readonly http = inject(HttpClient);
  private readonly gestion = `${API_URL}/rotaciones/gestion`;
  gestionar(params: Parametros) {
    return this.http.get<Pagina<RotacionGestion>>(this.gestion, { params });
  }
  catalogos() {
    return this.http.get<CatalogosRotacion>(this.gestion + '/catalogos');
  }
  private ruta(r: RotacionDatos) {
    return `${this.gestion}/${r.asignaturaId}/${r.inicio}/${r.fin}`;
  }
  crear(d: RotacionDatos) {
    return this.http.post<RotacionGestion>(this.gestion, d);
  }
  editar(r: RotacionDatos, d: RotacionDatos) {
    return this.http.put<RotacionGestion>(this.ruta(r), d);
  }
  estado(r: RotacionDatos, activa: boolean) {
    return this.http.patch<RotacionGestion>(this.ruta(r) + '/estado', { activa });
  }
  eliminar(r: RotacionDatos) {
    return this.http.delete<void>(this.ruta(r));
  }
  validar(filas: FilaRotacion[]) {
    return this.http.post<ResultadoRotaciones>(this.gestion + '/importar/validar', { filas });
  }
  importar(filas: FilaRotacion[]) {
    return this.http.post<ResultadoRotaciones>(this.gestion + '/importar', { filas });
  }
  listar(params: Parametros) {
    return this.http.get<Pagina<Rotacion>>(`${API_URL}/rotaciones`, { params });
  }
}
