import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DetalleMovimiento, RegistroHistorial } from '../models/historial.model';
import { Pagina, Parametros } from '../models/pagina.model';
import { API_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class HistorialService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/historial`;

  /** GET /historial (ConsultaController) */
  listar(params: Parametros): Observable<Pagina<RegistroHistorial>> {
    return this.http.get<Pagina<RegistroHistorial>>(this.url, { params });
  }

  /** GET /historial/{id} (HistorialAuditoriaController) */
  consultar(id: number): Observable<DetalleMovimiento> {
    return this.http.get<DetalleMovimiento>(`${this.url}/${id}`);
  }

  /** POST /historial/importaciones/{lote}/cancelar (HistorialAuditoriaController) */
  cancelarImportacion(idLote: string): Observable<void> {
    return this.http.post<void>(this.urlCancelarImportacion(idLote), null);
  }

  /** Misma ruta que `cancelarImportacion`, para `navigator.sendBeacon` al cerrar la pestaña. */
  urlCancelarImportacion(idLote: string): string {
    return `${this.url}/importaciones/${idLote}/cancelar`;
  }
}
