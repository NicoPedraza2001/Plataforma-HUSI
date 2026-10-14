import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DashboardService } from './dashboard.service';
describe('DashboardService', () => {
  let servicio: DashboardService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(DashboardService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('envía los filtros del corte al consultar', () => {
    servicio.consultar({ periodo: '2025-2', programa: 'Geriatría', asignaturaId: '8' }).subscribe();
    const solicitud = http.expectOne((peticion) => peticion.url === '/api/dashboard');
    expect(solicitud.request.method).toBe('GET');
    expect(solicitud.request.params.get('periodo')).toBe('2025-2');
    expect(solicitud.request.params.get('programa')).toBe('Geriatría');
    expect(solicitud.request.params.get('asignaturaId')).toBe('8');
    solicitud.flush({});
  });
  it('pide las relaciones entre programa, servicio y asignatura', () => {
    servicio.relaciones().subscribe();
    const solicitud = http.expectOne('/api/dashboard/relaciones');
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush([]);
  });
});
