import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { Perfil } from '../../../models/sesion.model';
import { SesionActual } from '../../../services/sesion-actual';
import { AdminLayout } from './admin-layout';

describe('AdminLayout', () => {
  let component: AdminLayout;
  let fixture: ComponentFixture<AdminLayout>;
  const sesionActual = {
    perfil: signal<Perfil | null>({
      nombres: 'Administrador',
      apellidos: 'Sistema',
      correo: 'admin@husi.com',
      rol: 'Administrador',
    }),
    cerrar: vi.fn().mockResolvedValue(undefined),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SesionActual, useValue: sesionActual }],
      imports: [AdminLayout],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('muestra el usuario en sesión y al cerrar sesión vuelve al login', async () => {
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const elemento: HTMLElement = fixture.nativeElement;
    expect(elemento.textContent).toContain('Administrador Sistema');

    elemento.querySelector<HTMLButtonElement>('.sidebar__salir')!.click();
    await fixture.whenStable();

    expect(sesionActual.cerrar).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
