import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { HistorialService } from './historial.service';

describe('HistorialService', () => {
  let service: HistorialService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(HistorialService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista con los filtros como parámetros de la URL', () => {
    service.listar({ q: 'ana', pagina: 2, tamano: 8, modulo: 'Residentes' }).subscribe();
    const peticion = http.expectOne((request) => request.url === '/api/historial');
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('modulo')).toBe('Residentes');
    expect(peticion.request.params.get('pagina')).toBe('2');
  });

  it('consulta el detalle de un movimiento', () => {
    service.consultar(7).subscribe();
    http.expectOne({ method: 'GET', url: '/api/historial/7' });
  });

  it('cancela una importación por su lote', () => {
    service.cancelarImportacion('lote-1').subscribe();
    http.expectOne({ method: 'POST', url: '/api/historial/importaciones/lote-1/cancelar' });
  });
});
