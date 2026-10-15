import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { Perfil } from '../../../models/sesion.model';
import { SesionActual } from '../../../services/sesion-actual';
import { CoordinatorLayout } from './coordinator-layout';

describe('CoordinatorLayout', () => {
  let component: CoordinatorLayout;
  let fixture: ComponentFixture<CoordinatorLayout>;
  const sesionActual = {
    perfil: signal<Perfil | null>({
      nombres: 'Gabriela',
      apellidos: 'Pérez',
      correo: 'gabriela.perez@husi.org.co',
      rol: 'Coordinador',
    }),
    cerrar: vi.fn().mockResolvedValue(undefined),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SesionActual, useValue: sesionActual }],
      imports: [CoordinatorLayout],
    }).compileComponents();

    fixture = TestBed.createComponent(CoordinatorLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('muestra solo el menú del coordinador y al cerrar sesión vuelve al login', async () => {
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const elemento: HTMLElement = fixture.nativeElement;
    expect(elemento.textContent).toContain('Gabriela Pérez');
    expect(elemento.textContent).toContain('Mis residentes');
    expect(elemento.textContent).not.toContain('Importar Excel');

    elemento.querySelector<HTMLButtonElement>('.sidebar__salir')!.click();
    await fixture.whenStable();

    expect(sesionActual.cerrar).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
