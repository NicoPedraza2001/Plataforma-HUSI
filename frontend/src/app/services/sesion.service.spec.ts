import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SesionService } from './sesion.service';

describe('SesionService', () => {
  let servicio: SesionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(SesionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('inicia sesión enviando correo y contraseña en el cuerpo, nunca en la URL', () => {
    servicio.iniciarSesion({ correo: 'admin@husi.com', contrasena: 'secreta-123' }).subscribe();
    const solicitud = http.expectOne('/api/sesion/iniciar');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual({ correo: 'admin@husi.com', contrasena: 'secreta-123' });
    expect(solicitud.request.urlWithParams).not.toContain('secreta');
    solicitud.flush({});
  });

  it('cierra sesión y consulta el perfil', () => {
    servicio.cerrarSesion().subscribe();
    expect(http.expectOne('/api/sesion/cerrar').request.method).toBe('POST');

    servicio.perfil().subscribe();
    expect(http.expectOne('/api/sesion/perfil').request.method).toBe('GET');
  });

  it('actualiza solo los datos del perfil que se envían', () => {
    servicio.actualizarPerfil({ apellidos: 'Gómez' }).subscribe();
    const solicitud = http.expectOne('/api/sesion/perfil');
    expect(solicitud.request.method).toBe('PATCH');
    expect(solicitud.request.body).toEqual({ apellidos: 'Gómez' });
    solicitud.flush({});
  });

  it('actualiza las credenciales con la contraseña actual en el cuerpo', () => {
    const cambio = { contrasenaActual: 'actual-123', contrasenaNueva: 'nueva-1234' };
    servicio.actualizarCredenciales(cambio).subscribe();
    const solicitud = http.expectOne('/api/sesion/credenciales');
    expect(solicitud.request.method).toBe('PATCH');
    expect(solicitud.request.body).toEqual(cambio);
    expect(solicitud.request.urlWithParams).not.toContain('actual-123');
    solicitud.flush(null, { status: 204, statusText: 'No Content' });
  });
});
