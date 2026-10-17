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

  it('pide los residentes a cargo sin enviar el servicio', () => {
    servicio.listarResidentes().subscribe();
    const solicitud = http.expectOne('/api/coordinador/residentes');
    expect(solicitud.request.method).toBe('GET');
    expect(solicitud.request.params.keys()).toEqual([]);
    solicitud.flush({ servicio: 'Urgencias', fecha: '2026-10-14', residentes: [], proximos: [] });
  });
});
