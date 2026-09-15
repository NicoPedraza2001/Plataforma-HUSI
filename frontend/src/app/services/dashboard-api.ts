import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Parametros } from './consulta';
export interface DashboardDatos {
 indicadores: {id: string; etiqueta: string; valor: string; detalle: string; delta?: string; tono: string}[];
 rotacionesPorServicio: {etiqueta: string; valor: number; maximo: number}[];
 ocupacionPorServicio: {etiqueta: string; valor: number; maximo: number}[];
 distribucionUniversidad: {etiqueta: string; porcentaje: number}[];
 tendenciaInicios: {etiqueta: string; valor: number}[];
 alertas: {id: string; nivel: string; etiqueta: string; detalle: string; cantidad: number}[];
 totalRegistros: number;
}
@Injectable({providedIn: 'root'})
export class DashboardApi {
 private readonly http=inject(HttpClient);
 consultar(params: Parametros) { return this.http.get<DashboardDatos>('/api/dashboard',{params}); }
}
