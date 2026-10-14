import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Perfil } from '../../../models/sesion.model';
import { SesionService } from '../../../services/sesion.service';
import { Ajustes } from './ajustes';

const PERFIL: Perfil = {
  nombres: 'Administrador',
  apellidos: 'Sistema',
  correo: 'admin@husi.com',
  rol: 'Administrador',
};

describe('Ajustes', () => {
  let component: Ajustes;
  let fixture: ComponentFixture<Ajustes>;
  let pagina: HTMLElement;
  let sesionService: {
    perfil: ReturnType<typeof vi.fn>;
    actualizarPerfil: ReturnType<typeof vi.fn>;
    actualizarCredenciales: ReturnType<typeof vi.fn>;
  };

  const boton = (texto: string) =>
    Array.from(pagina.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === texto && !b.disabled,
    ) as HTMLButtonElement;

  const escribir = async (id: string, valor: string) => {
    const input = pagina.querySelector<HTMLInputElement>(`#${id}`)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  };

  beforeEach(async () => {
    sesionService = {
      perfil: vi.fn(() => of(PERFIL)),
      actualizarPerfil: vi.fn(() => of({ ...PERFIL, apellidos: 'Gómez' })),
      actualizarCredenciales: vi.fn(() => of(PERFIL)),
    };
    await TestBed.configureTestingModule({
      providers: [{ provide: SesionService, useValue: sesionService }],
      imports: [Ajustes],
    }).compileComponents();

    fixture = TestBed.createComponent(Ajustes);
    component = fixture.componentInstance;
    pagina = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(pagina.textContent).toContain('Administrador Sistema');
  });

  it('envía solo el campo modificado tras confirmar', async () => {
    boton('Editar').click();
    await fixture.whenStable();
    await escribir('apellidos', 'Gómez');

    boton('Guardar cambios').click();
    await fixture.whenStable();
    expect(pagina.querySelector('[role="dialog"]')?.textContent).toContain('Gómez');
    expect(sesionService.actualizarPerfil).not.toHaveBeenCalled();

    boton('Sí, guardar cambios').click();
    await fixture.whenStable();
    expect(sesionService.actualizarPerfil).toHaveBeenCalledWith({ apellidos: 'Gómez' });
    expect(pagina.querySelector('[role="dialog"]')).toBeNull();
  });

  it('no deja guardar la contraseña si la confirmación no coincide', async () => {
    Array.from(pagina.querySelectorAll('button'))
      .filter((b) => b.textContent?.trim() === 'Editar')[1]
      .click();
    await fixture.whenStable();
    await escribir('contrasena-nueva', 'NuevaClave-1');
    await escribir('contrasena-confirmacion', 'OtraClave-1');

    expect(pagina.textContent).toContain('Las contraseñas no coinciden.');
    expect(boton('Guardar cambios')).toBeUndefined();

    await escribir('contrasena-confirmacion', 'NuevaClave-1');
    boton('Guardar cambios').click();
    await fixture.whenStable();
    await escribir('contrasena-actual', 'ClaveActual-1');
    boton('Sí, guardar cambios').click();
    await fixture.whenStable();

    expect(sesionService.actualizarCredenciales).toHaveBeenCalledWith({
      contrasenaActual: 'ClaveActual-1',
      contrasenaNueva: 'NuevaClave-1',
    });
  });
});
