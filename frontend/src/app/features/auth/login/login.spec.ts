import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';

import { Perfil } from '../../../models/sesion.model';
import { SesionActual } from '../../../services/sesion-actual';
import { Login } from './login';

const ADMINISTRADOR: Perfil = {
  nombres: 'Administrador',
  apellidos: 'Sistema',
  correo: 'admin@husi.com',
  rol: 'Administrador',
};

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let sesionActual: {
    consultar: ReturnType<typeof vi.fn>;
    iniciar: ReturnType<typeof vi.fn>;
    cerrar: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  beforeEach(async () => {
    sesionActual = {
      consultar: vi.fn().mockResolvedValue(null),
      iniciar: vi.fn(),
      cerrar: vi.fn().mockResolvedValue(undefined),
    };
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: SesionActual, useValue: sesionActual }],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  function escribir(correo: string, contrasena: string) {
    const elemento: HTMLElement = fixture.nativeElement;
    for (const [id, valor] of [
      ['correo', correo],
      ['contrasena', contrasena],
    ]) {
      const input = elemento.querySelector<HTMLInputElement>(`#${id}`)!;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
    }
  }

  async function enviar() {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('el administrador entra al panel admin', async () => {
    sesionActual.iniciar.mockResolvedValue(ADMINISTRADOR);
    escribir('admin@husi.com', 'secreta-123');
    await enviar();

    expect(sesionActual.iniciar).toHaveBeenCalledWith({
      correo: 'admin@husi.com',
      contrasena: 'secreta-123',
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/admin');
  });

  it('muestra el mensaje del backend y borra la contraseña si las credenciales fallan', async () => {
    sesionActual.iniciar.mockRejectedValue(
      new HttpErrorResponse({
        status: 401,
        error: { mensaje: 'Correo o contraseña incorrectos.', campos: {} },
      }),
    );
    escribir('admin@husi.com', 'errada-123');
    await enviar();

    const elemento: HTMLElement = fixture.nativeElement;
    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain(
      'Correo o contraseña incorrectos.',
    );
    expect(elemento.querySelector<HTMLInputElement>('#contrasena')!.value).toBe('');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('no consulta el backend si el formulario está incompleto', async () => {
    escribir('no-es-un-correo', '');
    await enviar();
    expect(sesionActual.iniciar).not.toHaveBeenCalled();
  });

  it('el coordinador entra a su panel', async () => {
    sesionActual.iniciar.mockResolvedValue({ ...ADMINISTRADOR, rol: 'Coordinador' });
    escribir('coordinador@husi.com', 'secreta-123');
    await enviar();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/coordinador');
  });

  it('un perfil sin panel construido cierra la sesión y lo informa', async () => {
    sesionActual.iniciar.mockResolvedValue({ ...ADMINISTRADOR, rol: 'Auditor' });
    escribir('auditor@husi.com', 'secreta-123');
    await enviar();

    expect(sesionActual.cerrar).toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Auditor aún no está disponible');
  });

  it('avisa que hay que ingresar con los datos nuevos tras cambiar las credenciales', async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        { provide: SesionActual, useValue: sesionActual },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ motivo: 'credenciales-actualizadas' }),
            },
          },
        },
      ],
    }).compileComponents();

    const conAviso = TestBed.createComponent(Login);
    await conAviso.whenStable();

    const elemento: HTMLElement = conAviso.nativeElement;
    expect(elemento.querySelector('[role="status"]')?.textContent).toContain(
      'Tus datos de acceso cambiaron',
    );
    expect(elemento.querySelector('[role="alert"]')).toBeNull();
  });
});
