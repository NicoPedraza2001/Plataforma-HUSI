import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LoteImportacion } from './lote-importacion';
import {
  CABECERA_ARCHIVO,
  CABECERA_LOTE,
  loteImportacionInterceptor,
} from './lote-importacion.interceptor';

describe('loteImportacionInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let loteImportacion: LoteImportacion;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([loteImportacionInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    loteImportacion = TestBed.inject(LoteImportacion);
  });

  afterEach(() => backend.verify());

  it('sin importación en curso no agrega cabeceras', () => {
    http.post('/api/residentes', {}).subscribe();
    expect(backend.expectOne('/api/residentes').request.headers.has(CABECERA_LOTE)).toBe(false);
  });

  it('durante una importación marca cada petición con el mismo lote y el archivo', () => {
    loteImportacion.iniciar('Rotación 2026-1.xlsx');
    http.post('/api/residentes', {}).subscribe();
    http.post('/api/universidades', {}).subscribe();

    const [residente, universidad] = [
      backend.expectOne('/api/residentes').request,
      backend.expectOne('/api/universidades').request,
    ];
    expect(residente.headers.get(CABECERA_LOTE)).toBe(loteImportacion.actual()?.id);
    expect(universidad.headers.get(CABECERA_LOTE)).toBe(residente.headers.get(CABECERA_LOTE));
    expect(residente.headers.get(CABECERA_ARCHIVO)).toBe('Rotaci%C3%B3n%202026-1.xlsx');

    loteImportacion.terminar();
    http.get('/api/historial').subscribe();
    expect(backend.expectOne('/api/historial').request.headers.has(CABECERA_LOTE)).toBe(false);
  });
});
