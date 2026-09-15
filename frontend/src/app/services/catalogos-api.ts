import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
export interface Catalogos { universidades: string[]; programas: string[]; servicios: string[]; roles: string[]; modulos: string[]; acciones: string[]; periodos: string[]; }
@Injectable({providedIn: 'root'})
export class CatalogosApi {
 private readonly http = inject(HttpClient);
 listar() { return this.http.get<Catalogos>('/api/catalogos'); }
}
