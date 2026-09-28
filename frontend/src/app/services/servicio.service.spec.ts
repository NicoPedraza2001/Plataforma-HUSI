import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ServicioService } from './servicio.service';
describe('ServicioService CRUD', () => {
  let api: ServicioService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ServicioService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('consulta y pagina en backend', () => {
    api.listar({ q: 'prueba', pagina: 2 }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/servicios');
    expect(req.request.params.get('q')).toBe('prueba');
    expect(req.request.params.get('pagina')).toBe('2');
    req.flush({ contenido: [] });
  });
  it('consulta detalle para editar', () => {
    api.consultar(23).subscribe();
    const req = http.expectOne('/api/servicios/23');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 23 });
  });
  it('cambia estado y borra en endpoints separados', () => {
    api.cambiarEstado(23, 'Inactivo').subscribe();
    const req = http.expectOne('/api/servicios/23/estado');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ estado: 'Inactivo' });
    req.flush({ id: 23, estado: 'Inactivo' });
    api.eliminarDefinitivamente(23).subscribe();
    const del = http.expectOne('/api/servicios/23/definitivo');
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });
  it('envía los campos al crear y editar', () => {
    const datos = {
      nombre: 'Servicio',
      descripcion: null,
      cupoSimultaneo: 2,
      cupoMaximoInstalado: 5,
    };
    api.crear(datos).subscribe();
    let req = http.expectOne('/api/servicios');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(datos);
    req.flush({ id: 23, ...datos });
    api.actualizar(23, datos).subscribe();
    req = http.expectOne('/api/servicios/23');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(datos);
    req.flush({ id: 23, ...datos });
  });
});
