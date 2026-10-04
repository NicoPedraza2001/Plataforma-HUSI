import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CoordinadorService } from './coordinador.service';
describe('CoordinadorService', () => {
  let servicio: CoordinadorService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(CoordinadorService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('envía búsqueda y paginación al backend', () => {
    servicio.listar({ q: 'Prueba', servicioId: 1, pagina: 2, tamano: 10 }).subscribe();
    const solicitud = http.expectOne((peticion) => peticion.url === '/api/coordinadores');
    expect(solicitud.request.method).toBe('GET');
    expect(solicitud.request.params.get('q')).toBe('Prueba');
    expect(solicitud.request.params.get('servicioId')).toBe('1');
    expect(solicitud.request.params.get('pagina')).toBe('2');
    solicitud.flush({ contenido: [], totalElementos: 0, totalPaginas: 1, pagina: 1, tamano: 10 });
  });
  it('usa los endpoints de escritura y consulta', () => {
    const datos = { usuarioId: 2, servicioId: 1 };
    servicio.crear(datos).subscribe();
    let solicitud = http.expectOne('/api/coordinadores');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual(datos);
    solicitud.flush({ id: 3 });
    servicio.consultar(3).subscribe();
    solicitud = http.expectOne('/api/coordinadores/3');
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush({ id: 3 });
    servicio.actualizar(3, datos).subscribe();
    solicitud = http.expectOne('/api/coordinadores/3');
    expect(solicitud.request.method).toBe('PUT');
    expect(solicitud.request.body).toEqual(datos);
    solicitud.flush({ id: 3 });
    servicio.cambiarEstado(3, 'Inactivo').subscribe();
    solicitud = http.expectOne('/api/coordinadores/3/estado');
    expect(solicitud.request.method).toBe('PATCH');
    expect(solicitud.request.body).toEqual({ estado: 'Inactivo' });
    solicitud.flush({ id: 3 });
    servicio.eliminarDefinitivamente(3).subscribe();
    solicitud = http.expectOne('/api/coordinadores/3/definitivo');
    expect(solicitud.request.method).toBe('DELETE');
    solicitud.flush(null);
  });
});
