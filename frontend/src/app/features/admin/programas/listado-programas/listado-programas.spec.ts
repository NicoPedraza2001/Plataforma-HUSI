import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ListadoProgramas } from './listado-programas';
import { ProgramasApi } from '../programas-api';
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
      listar: vi.fn(() =>
        falla ? throwError(() => new HttpErrorResponse({ status: 0 })) : of([programa]),
      ),
      universidades: vi.fn(() => of([{ id: 2, nombre: 'Universidad de prueba' }])),
      crear: vi.fn(() => of(programa)),
      actualizar: vi.fn(() => of(programa)),
      desactivar: vi.fn(() => of(undefined)),
    };
    TestBed.configureTestingModule({
      imports: [ListadoProgramas],
      providers: [{ provide: ProgramasApi, useValue: api }],
    });
    return { fixture: TestBed.createComponent(ListadoProgramas), api };
  }
  it('muestra datos del servidor y elimina solo después de confirmar', async () => {
    const { fixture, api } = configurar();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Universidad de prueba');
    fixture.nativeElement.querySelector('[aria-label="Eliminar Pediatría"]').click();
    fixture.detectChanges();
    expect(api.desactivar).not.toHaveBeenCalled();
    const botones = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    ) as HTMLButtonElement[];
    botones.find((b) => b.textContent?.trim() === 'Desactivar programa')!.click();
    await fixture.whenStable();
    expect(api.desactivar).toHaveBeenCalledWith(1);
  });
  it('explica un fallo de conexión y permite reintentar', async () => {
    const { fixture } = configurar(true);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos conectar');
    expect(fixture.nativeElement.textContent).toContain('Reintentar');
  });
});
