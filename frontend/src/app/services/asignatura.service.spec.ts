import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AsignaturaService } from './asignatura.service';
describe('AsignaturaService', () => {
  let api: AsignaturaService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AsignaturaService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('envía búsqueda y paginación al backend', () => {
    api.listar({ q: 'Prueba', servicioId: 1, pagina: 2, tamano: 10 }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/asignaturas');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('q')).toBe('Prueba');
    expect(req.request.params.get('servicioId')).toBe('1');
    expect(req.request.params.get('pagina')).toBe('2');
    req.flush({ contenido: [], totalElementos: 0, totalPaginas: 1, pagina: 1, tamano: 10 });
  });
  it('usa los endpoints de escritura y consulta', () => {
    const datos = { nombre: 'Materia', servicioId: 1 };
    api.crear(datos).subscribe();
    let req = http.expectOne('/api/asignaturas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(datos);
    req.flush({ id: 3 });
    api.consultar(3).subscribe();
    req = http.expectOne('/api/asignaturas/3');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 3 });
    api.actualizar(3, datos).subscribe();
    req = http.expectOne('/api/asignaturas/3');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(datos);
    req.flush({ id: 3 });
    api.cambiarEstado(3, 'Inactivo').subscribe();
    req = http.expectOne('/api/asignaturas/3/estado');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ estado: 'Inactivo' });
    req.flush({ id: 3 });
    api.eliminarDefinitivamente(3).subscribe();
    req = http.expectOne('/api/asignaturas/3/definitivo');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
