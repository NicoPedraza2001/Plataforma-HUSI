import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CargaInicialService } from './carga-inicial.service';
describe('CargaInicialService', () => {
  it('envía la confirmación y la huella al backend', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(CargaInicialService).confirmar([], true, 'revision').subscribe();
    const http = TestBed.inject(HttpTestingController),
      solicitud = http.expectOne('/api/rotaciones/carga-inicial/confirmar');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual({
      filas: [],
      crearFaltantes: true,
      validacion: 'revision',
    });
    solicitud.flush({});
    http.verify();
  });
});
