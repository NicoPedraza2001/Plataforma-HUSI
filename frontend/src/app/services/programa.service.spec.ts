import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProgramaService } from './programa.service';

describe('ProgramaService', () => {
  let service: ProgramaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProgramaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('usa las rutas y verbos de ProgramaController', () => {
    const datos = { nombre: 'Pediatría', universidadId: 2 };
    const filas = [{ fila: 3, nombre: 'Pediatría', universidad: 'Universidad A' }];

    service.listar().subscribe();
    service.consultar(1).subscribe();
    service.crear(datos).subscribe();
    service.actualizar(1, datos).subscribe();
    service.desactivar(1).subscribe();
    service.validar(filas).subscribe();
    service.importar(filas).subscribe();

    http.expectOne({ method: 'GET', url: '/api/programas' });
    http.expectOne({ method: 'GET', url: '/api/programas/1' });
    expect(http.expectOne({ method: 'POST', url: '/api/programas' }).request.body).toEqual(datos);
    expect(http.expectOne({ method: 'PUT', url: '/api/programas/1' }).request.body).toEqual(datos);
    http.expectOne({ method: 'DELETE', url: '/api/programas/1' });
    expect(
      http.expectOne({ method: 'POST', url: '/api/programas/importacion/validar' }).request.body,
    ).toEqual({ filas });
    expect(
      http.expectOne({ method: 'POST', url: '/api/programas/importacion' }).request.body,
    ).toEqual({ filas });
  });
});
