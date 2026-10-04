import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AsignaturaService } from '../../../../services/asignatura.service';
import { ServicioService } from '../../../../services/servicio.service';
import { ListadoAsignaturas } from './listado-asignaturas';
describe('ListadoAsignaturas', () => {
  function preparar(error = false) {
    const api = {
      listar: () =>
        error
          ? throwError(() => new HttpErrorResponse({ status: 0 }))
          : of({ contenido: [], totalElementos: 0, totalPaginas: 1, pagina: 1, tamano: 10 }),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: AsignaturaService, useValue: api },
        { provide: ServicioService, useValue: { catalogo: () => of([]) } },
      ],
    });
    return TestBed.createComponent(ListadoAsignaturas);
  }
  it('muestra un listado vacío sin inventar registros', async () => {
    const fixture = preparar();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se encontraron asignaturas');
  });
  it('muestra el error de conexión y permite reintentar', async () => {
    const fixture = preparar(true);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=alert]')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Reintentar');
    expect(fixture.nativeElement.textContent).not.toContain('No se encontraron asignaturas');
  });
});
