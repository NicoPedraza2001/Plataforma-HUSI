import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ServicioService } from './servicio.service';
describe('ServicioService CRUD', () => {
  let servicio: ServicioService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(ServicioService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('consulta y pagina en backend', () => {
    servicio.listar({ q: 'prueba', pagina: 2 }).subscribe();
    const solicitud = http.expectOne((peticion) => peticion.url === '/api/servicios');
    expect(solicitud.request.params.get('q')).toBe('prueba');
    expect(solicitud.request.params.get('pagina')).toBe('2');
    solicitud.flush({ contenido: [] });
  });
  it('consulta detalle para editar', () => {
    servicio.consultar(23).subscribe();
    const solicitud = http.expectOne('/api/servicios/23');
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush({ id: 23 });
  });
  it('cambia estado y borra en endpoints separados', () => {
    servicio.cambiarEstado(23, 'Inactivo').subscribe();
    const solicitud = http.expectOne('/api/servicios/23/estado');
    expect(solicitud.request.method).toBe('PATCH');
    expect(solicitud.request.body).toEqual({ estado: 'Inactivo' });
    solicitud.flush({ id: 23, estado: 'Inactivo' });
    servicio.eliminarDefinitivamente(23).subscribe();
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
    servicio.crear(datos).subscribe();
    let solicitud = http.expectOne('/api/servicios');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual(datos);
    solicitud.flush({ id: 23, ...datos });
    servicio.actualizar(23, datos).subscribe();
    solicitud = http.expectOne('/api/servicios/23');
    expect(solicitud.request.method).toBe('PUT');
    expect(solicitud.request.body).toEqual(datos);
    solicitud.flush({ id: 23, ...datos });
  });
});
