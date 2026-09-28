import { DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, catchError, of, switchMap, tap, timer } from 'rxjs';
import { Parametros } from '../../models/pagina.model';
import { mensajeError } from '../../services/api-error';
/** Cancela inmediatamente la consulta anterior, incluso durante la espera al escribir. */
export class Consulta<T> {
  readonly datos = signal<T | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  private readonly solicitudes = new Subject<{ params: Parametros; espera: number }>();
  private ultima: Parametros = {};

  constructor(peticion: (params: Parametros) => Observable<T>) {
    this.solicitudes
      .pipe(
        tap(() => {
          this.datos.set(null);
          this.error.set('');
          this.cargando.set(true);
        }),
        switchMap(({ params, espera }) =>
          (espera ? timer(espera) : of(0)).pipe(
            switchMap(() => peticion(params)),
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

  cargar(params: Parametros = this.ultima, espera = 0) {
    this.ultima = { ...params };
    this.solicitudes.next({ params: this.ultima, espera });
  }
}
