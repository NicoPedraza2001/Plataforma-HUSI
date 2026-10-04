import { DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, catchError, of, switchMap, tap, timer } from 'rxjs';
import { Parametros } from '../../models/pagina.model';
import { mensajeError } from '../../services/api-error';
/**
 * Estado de una consulta al backend (datos, cargando, error). Cancela inmediatamente la
 * consulta anterior, incluso durante la demora al escribir en un buscador.
 */
export class Consulta<T> {
  readonly datos = signal<T | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  private readonly solicitudes = new Subject<{ params: Parametros; demoraMs: number }>();
  private ultimosParametros: Parametros = {};

  constructor(consultarBackend: (params: Parametros) => Observable<T>) {
    this.solicitudes
      .pipe(
        tap(() => {
          this.datos.set(null);
          this.error.set('');
          this.cargando.set(true);
        }),
        switchMap(({ params, demoraMs }) =>
          (demoraMs ? timer(demoraMs) : of(0)).pipe(
            switchMap(() => consultarBackend(params)),
            catchError((e) => {
              this.error.set(mensajeError(e));
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((datos) => {
        this.datos.set(datos);
        this.cargando.set(false);
      });
  }

  /** Sin parámetros repite la última consulta; `demoraMs` espera antes de pedir (buscadores). */
  cargar(params: Parametros = this.ultimosParametros, demoraMs = 0) {
    this.ultimosParametros = { ...params };
    this.solicitudes.next({ params: this.ultimosParametros, demoraMs });
  }
}
