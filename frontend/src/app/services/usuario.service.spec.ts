import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UsuarioService } from './usuario.service';
describe('UsuarioService CRUD', () => {
  let servicio: UsuarioService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(UsuarioService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('consulta y pagina en backend', () => {
    servicio.listar({ q: 'prueba', pagina: 2 }).subscribe();
    const solicitud = http.expectOne((peticion) => peticion.url === '/api/usuarios');
    expect(solicitud.request.params.get('q')).toBe('prueba');
    expect(solicitud.request.params.get('pagina')).toBe('2');
    solicitud.flush({ contenido: [] });
  });
  it('consulta detalle para editar', () => {
    servicio.consultar(23).subscribe();
    const solicitud = http.expectOne('/api/usuarios/23');
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush({ id: 23 });
  });
  it('cambia estado y borra en endpoints separados', () => {
    servicio.cambiarEstado(23, 'Inactivo').subscribe();
    const solicitud = http.expectOne('/api/usuarios/23/estado');
    expect(solicitud.request.method).toBe('PATCH');
    expect(solicitud.request.body).toEqual({ estado: 'Inactivo' });
    solicitud.flush({ id: 23, estado: 'Inactivo' });
    servicio.eliminarDefinitivamente(23).subscribe();
    const del = http.expectOne('/api/usuarios/23/definitivo');
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });
});
