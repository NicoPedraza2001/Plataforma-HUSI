import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { SesionActual } from './sesion-actual';
import { sesionExpiradaInterceptor } from './sesion-expirada.interceptor';

describe('sesionExpiradaInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let router: Router;
  const olvidar = vi.fn();

  beforeEach(() => {
    olvidar.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([sesionExpiradaInterceptor])),
        provideHttpClientTesting(),
        { provide: SesionActual, useValue: { olvidar } },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  it('un 401 en medio del trabajo olvida la sesión y lleva al login', () => {
    http.get('/api/universidades').subscribe({ error: () => undefined });
    backend.expectOne('/api/universidades').flush(null, { status: 401, statusText: '' });

    expect(olvidar).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { volver: '/', motivo: 'expirada' },
    });
  });

  it('un 401 del inicio de sesión lo maneja el login, no el interceptor', () => {
    http.post('/api/sesion/iniciar', {}).subscribe({ error: () => undefined });
    backend.expectOne('/api/sesion/iniciar').flush(null, { status: 401, statusText: '' });

    expect(olvidar).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('otros errores pasan sin cambios', () => {
    http.get('/api/usuarios').subscribe({ error: () => undefined });
    backend.expectOne('/api/usuarios').flush(null, { status: 403, statusText: '' });
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
