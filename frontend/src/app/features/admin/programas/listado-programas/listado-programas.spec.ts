import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ProgramaService } from '../../../../services/programa.service';
import { UniversidadService } from '../../../../services/universidad.service';
import { ListadoProgramas } from './listado-programas';
describe('Programas conectado', () => {
  const programa = {
    id: 1,
    nombre: 'Pediatría',
    universidadId: 2,
    universidad: 'Universidad de prueba',
    estado: 'Activo',
  };
  function configurar(falla = false) {
    const api = {
      buscar: vi.fn(() =>
        falla
          ? throwError(() => new HttpErrorResponse({ status: 0 }))
          : of({
              contenido: [programa],
              totalElementos: 1,
              totalPaginas: 1,
              pagina: 1,
              tamano: 10,
            }),
      ),
      listar: vi.fn(() => of([{ id: 2, nombre: 'Universidad de prueba' }])),
      crear: vi.fn(() => of(programa)),
      actualizar: vi.fn(() => of(programa)),
      eliminarDefinitivamente: vi.fn(() => of(undefined)),
    };
    TestBed.configureTestingModule({
      imports: [ListadoProgramas],
      providers: [
        provideRouter([]),
        { provide: ProgramaService, useValue: api },
        { provide: UniversidadService, useValue: api },
      ],
    });
    return { fixture: TestBed.createComponent(ListadoProgramas), api };
  }
  it('muestra datos del servidor y elimina solo después de confirmar', async () => {
    const { fixture, api } = configurar();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Universidad de prueba');
    fixture.nativeElement
      .querySelector('[aria-label="Eliminar definitivamente Pediatría"]')
      .click();
    fixture.detectChanges();
    expect(api.eliminarDefinitivamente).not.toHaveBeenCalled();
    const botones = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    ) as HTMLButtonElement[];
    botones.find((b) => b.textContent?.trim() === 'Eliminar definitivamente')!.click();
    await fixture.whenStable();
    expect(api.eliminarDefinitivamente).toHaveBeenCalledWith(1);
  });
  it('explica un fallo de conexión y permite reintentar', async () => {
    const { fixture } = configurar(true);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos conectar');
    expect(fixture.nativeElement.textContent).toContain('Reintentar');
  });
});
