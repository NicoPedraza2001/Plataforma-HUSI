import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
export interface Programa {
  id: number;
  nombre: string;
  universidadId: number;
  universidad: string;
  estado: string;
}
export interface Universidad {
  id: number;
  nombre: string;
}
export interface ProgramaDatos {
  nombre: string;
  universidadId: number;
}
export interface FilaImportacion {
  fila: number;
  nombre: string;
  universidad: string;
}
export interface ResultadoFila extends FilaImportacion {
  estado: 'VALIDO' | 'CREADO' | 'OMITIDO' | 'ERROR';
  mensaje: string;
  programaId?: number;
}
export interface ResultadoImportacion {
  total: number;
  validos: number;
  creados: number;
  omitidos: number;
  errores: number;
  filas: ResultadoFila[];
}
export function mensajeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0 || error.status === 502 || error.status === 504)
      return 'No pudimos conectar con el servidor. Comprueba que el backend esté encendido e intenta de nuevo.';
    const detalle = error.error;
    if (detalle?.mensaje)
      return [detalle.mensaje, ...Object.values(detalle.campos ?? {})].join(' ');
  }
  return 'No se pudo completar la operación. Intenta de nuevo.';
}
@Injectable({ providedIn: 'root' })
export class ProgramasApi {
  private readonly http = inject(HttpClient);
  listar() {
    return this.http.get<Programa[]>('/api/programas');
  }
  universidades() {
    return this.http.get<Universidad[]>('/api/universidades');
  }
  crear(datos: ProgramaDatos) {
    return this.http.post<Programa>('/api/programas', datos);
  }
  actualizar(id: number, datos: ProgramaDatos) {
    return this.http.put<Programa>('/api/programas/' + id, datos);
  }
  desactivar(id: number) {
    return this.http.delete<void>('/api/programas/' + id);
  }
  validar(filas: FilaImportacion[]) {
    return this.http.post<ResultadoImportacion>('/api/programas/importacion/validar', { filas });
  }
  importar(filas: FilaImportacion[]) {
    return this.http.post<ResultadoImportacion>('/api/programas/importacion', { filas });
  }
}
