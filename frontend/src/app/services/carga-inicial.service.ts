import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FilaCargaInicial, ResultadoCargaInicial } from '../models/carga-inicial.model';
import { API_URL } from './api.config';
@Injectable({ providedIn: 'root' })
export class CargaInicialService {
  private readonly http = inject(HttpClient);
  private readonly url = API_URL + '/rotaciones/carga-inicial';
  /** POST /api/rotaciones/carga-inicial/validar */
  validar(filas: FilaCargaInicial[], crearFaltantes: boolean) {
    return this.http.post<ResultadoCargaInicial>(this.url + '/validar', { filas, crearFaltantes });
  }
  /** POST /api/rotaciones/carga-inicial/confirmar */
  confirmar(filas: FilaCargaInicial[], crearFaltantes: boolean, validacion: string) {
    return this.http.post<ResultadoCargaInicial>(this.url + '/confirmar', {
      filas,
      crearFaltantes,
      validacion,
    });
  }
}
