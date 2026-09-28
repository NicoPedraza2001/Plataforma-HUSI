import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CoordinadorService } from '../../../../services/coordinador.service';
import { ServicioService } from '../../../../services/servicio.service';
import { ListadoCoordinadores } from './listado-coordinadores';
describe('ListadoCoordinadores', () => {
  function preparar(error = false) {
    const api = {
      listar: () =>
        error
          ? throwError(() => new HttpErrorResponse({ status: 0 }))
          : of({ contenido: [], totalElementos: 0, totalPaginas: 1, pagina: 1, tamano: 10 }),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: CoordinadorService, useValue: api },
        { provide: ServicioService, useValue: { catalogo: () => of([]) } },
      ],
    });
    return TestBed.createComponent(ListadoCoordinadores);
  }
  it('muestra un listado vacío sin inventar registros', async () => {
    const f = preparar();
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('No se encontraron coordinadores');
  });
  it('muestra el error de conexión y permite reintentar', async () => {
    const f = preparar(true);
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('[role=alert]')).toBeTruthy();
    expect(f.nativeElement.textContent).toContain('Reintentar');
    expect(f.nativeElement.textContent).not.toContain('No se encontraron coordinadores');
  });
});
