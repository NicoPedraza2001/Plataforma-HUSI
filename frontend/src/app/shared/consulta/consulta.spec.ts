import { TestBed } from '@angular/core/testing';
import { provideHttpClient, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Consulta } from './consulta';
import { Pagina } from '../../models/pagina.model';
import { ProgramaService } from '../../services/programa.service';
describe('Consultas remotas', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }),
  );
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    vi.useRealTimers();
  });
  it('envía búsqueda y página al servidor sin recortar resultados localmente', () => {
    const api = TestBed.inject(ProgramaService);
    let nombres: string[] = [];
    api
      .buscar({ q: 'cardio', pagina: 2, tamano: 10 })
      .subscribe((p) => (nombres = p.contenido.map((x) => x.nombre)));
    const req = TestBed.inject(HttpTestingController).expectOne(
      (r) =>
        r.url === '/api/programas/buscar' &&
        r.params.get('q') === 'cardio' &&
        r.params.get('pagina') === '2',
    );
    req.flush({
      contenido: [{ id: 11, nombre: 'Nombre entregado por servidor' }],
      totalElementos: 11,
      totalPaginas: 2,
      pagina: 2,
      tamano: 10,
    });
    expect(nombres).toEqual(['Nombre entregado por servidor']);
  });
  it('cancela respuestas anteriores, espera al escribir y se recupera de errores', () => {
    vi.useFakeTimers();
    const http = TestBed.inject(HttpClient),
      control = TestBed.inject(HttpTestingController);
    const consulta = TestBed.runInInjectionContext(
      () =>
        new Consulta<Pagina<string>>((params) =>
          http.get<Pagina<string>>('/api/prueba', { params }),
        ),
    );
    consulta.cargar({ q: 'antes' });
    const anterior = control.expectOne('/api/prueba?q=antes');
    consulta.cargar({ q: 'nuevo' }, 300);
    expect(anterior.cancelled).toBe(true);
    expect(consulta.cargando()).toBe(true);
    vi.advanceTimersByTime(299);
    control.expectNone('/api/prueba?q=nuevo');
    vi.advanceTimersByTime(1);
    control.expectOne('/api/prueba?q=nuevo').error(new ProgressEvent('error'));
    expect(consulta.error()).toContain('No pudimos conectar');
    expect(consulta.datos()).toBeNull();
    expect(consulta.cargando()).toBe(false);
    consulta.cargar();
    control
      .expectOne('/api/prueba?q=nuevo')
      .flush({ contenido: [], totalElementos: 0, totalPaginas: 1, pagina: 1, tamano: 10 });
    expect(consulta.error()).toBe('');
    expect(consulta.datos()?.contenido).toEqual([]);
  });
});
