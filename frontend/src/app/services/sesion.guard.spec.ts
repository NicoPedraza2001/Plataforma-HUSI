import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { Perfil } from '../models/sesion.model';
import { SesionActual } from './sesion-actual';
import { requiereRol } from './sesion.guard';

describe('requiereRol', () => {
  const consultar = vi.fn<() => Promise<Perfil | null>>();

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SesionActual, useValue: { consultar } }],
    });
  });

  function navegarA(url: string) {
    return TestBed.runInInjectionContext(() =>
      requiereRol('Administrador')({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    ) as Promise<boolean | UrlTree>;
  }

  function perfilCon(rol: string): Perfil {
    return { nombres: 'N', apellidos: 'A', correo: 'n@husi.com', rol };
  }

  it('deja pasar al rol indicado', async () => {
    consultar.mockResolvedValue(perfilCon('Administrador'));
    expect(await navegarA('/admin/residentes')).toBe(true);
  });

  it('sin sesión lleva al login recordando la pantalla pedida', async () => {
    consultar.mockResolvedValue(null);
    const destino = await navegarA('/admin/residentes');
    expect(TestBed.inject(Router).serializeUrl(destino as UrlTree)).toBe(
      '/login?volver=%2Fadmin%2Fresidentes',
    );
  });

  it('otro rol no entra al panel', async () => {
    consultar.mockResolvedValue(perfilCon('Coordinador'));
    const destino = await navegarA('/admin/usuarios');
    expect(TestBed.inject(Router).serializeUrl(destino as UrlTree)).toBe('/login');
  });

  it('si el backend no responde, lleva al login', async () => {
    consultar.mockRejectedValue(new Error('sin conexión'));
    expect(await navegarA('/admin')).toBeInstanceOf(UrlTree);
  });
});
