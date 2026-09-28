import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UsuarioService } from './usuario.service';
describe('UsuarioService CRUD', () => {
  let api: UsuarioService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(UsuarioService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('consulta y pagina en backend', () => {
    api.listar({ q: 'prueba', pagina: 2 }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/usuarios');
    expect(req.request.params.get('q')).toBe('prueba');
    expect(req.request.params.get('pagina')).toBe('2');
    req.flush({ contenido: [] });
  });
  it('consulta detalle para editar', () => {
    api.consultar(23).subscribe();
    const req = http.expectOne('/api/usuarios/23');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 23 });
  });
  it('cambia estado y borra en endpoints separados', () => {
    api.cambiarEstado(23, 'Inactivo').subscribe();
    const req = http.expectOne('/api/usuarios/23/estado');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ estado: 'Inactivo' });
    req.flush({ id: 23, estado: 'Inactivo' });
    api.eliminarDefinitivamente(23).subscribe();
    const del = http.expectOne('/api/usuarios/23/definitivo');
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });
});
