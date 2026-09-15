import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Pagina, Parametros } from './consulta';
export interface Rotacion { id: string; residente: string; universidad: string; servicio: string; asignatura: string; doctor: string; inicio: string; fin: string; estado: string; }
@Injectable({providedIn:'root'})
export class RotacionesApi {
 private readonly http=inject(HttpClient);
 private readonly gestion='/api/rotaciones/gestion';
 gestionar(params: Parametros) { return this.http.get<Pagina<RotacionGestion>>(this.gestion,{params}); }
 catalogos() { return this.http.get<CatalogosRotacion>(this.gestion+'/catalogos'); }
 private ruta(r: RotacionDatos) { return `${this.gestion}/${r.asignaturaId}/${r.inicio}/${r.fin}`; }
 crear(d: RotacionDatos) { return this.http.post<RotacionGestion>(this.gestion,d); }
 editar(r: RotacionDatos,d: RotacionDatos) { return this.http.put<RotacionGestion>(this.ruta(r),d); }
 estado(r: RotacionDatos,activa: boolean) { return this.http.patch<RotacionGestion>(this.ruta(r)+'/estado',{activa}); }
 eliminar(r: RotacionDatos) { return this.http.delete<void>(this.ruta(r)); }
 validar(filas: FilaRotacion[]) { return this.http.post<ResultadoRotaciones>(this.gestion+'/importar/validar',{filas}); }
 importar(filas: FilaRotacion[]) { return this.http.post<ResultadoRotaciones>(this.gestion+'/importar',{filas}); }
 listar(params: Parametros) { return this.http.get<Pagina<Rotacion>>('/api/rotaciones',{params}); }
}

export interface RotacionDatos { asignaturaId: number; inicio: string; fin: string; }
export interface RotacionGestion extends RotacionDatos { asignatura: string; servicio: string; servicioId: number; activa: boolean; disponibilidad: string; cupoDisponible: number; relaciones: number; }
export interface CatalogosRotacion { asignaturas: {id:number; nombre:string; servicio:string; servicioId:number}[]; servicios: {id:number; nombre:string}[]; }
export interface FilaRotacion { fila: number; asignatura: string; servicio: string; inicio: string; fin: string; }
export interface ResultadoRotaciones { total: number; VALIDO:number; CREADO:number; OMITIDO:number; ERROR:number; filas: {fila:number;estado:string;mensaje:string}[]; }
