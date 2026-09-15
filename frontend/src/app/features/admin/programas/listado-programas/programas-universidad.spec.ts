import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ListadoProgramas } from './listado-programas';

describe('Programas de una universidad', () => {
  it('consulta por ID y lo conserva al crear un programa, sin importación', async () => {
    TestBed.configureTestingModule({
      imports: [ListadoProgramas],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ universidadId: '72' })) },
        },
      ],
    });
    const fixture = TestBed.createComponent(ListadoProgramas),
      http = TestBed.inject(HttpTestingController);
    const pagina = { contenido: [], totalElementos: 0, totalPaginas: 1, pagina: 1, tamano: 10 };
    http.expectOne((r) => r.url === '/api/universidades/72/programas').flush(pagina);
    http.expectOne('/api/universidades').flush([{ id: 72, nombre: 'Universidad de prueba' }]);
    http
      .expectOne('/api/universidades/72')
      .flush({ id: 72, nombre: 'Universidad de prueba', pais: 'Colombia', estado: 'Activo' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'Esta universidad no tiene programas registrados.',
    );
    expect(fixture.nativeElement.textContent).not.toContain('Importar Excel');
    (fixture.nativeElement.querySelector('.btn--add') as HTMLButtonElement).click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#universidad-programa').disabled).toBe(true);
    const input = fixture.nativeElement.querySelector('#nombre-programa') as HTMLInputElement;
    input.value = 'Programa nuevo';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    const post = http.expectOne((r) => r.url === '/api/programas' && r.method === 'POST');
    expect(post.request.body).toEqual({ nombre: 'Programa nuevo', universidadId: 72 });
    post.flush({
      id: 90,
      nombre: 'Programa nuevo',
      universidadId: 72,
      universidad: 'Universidad de prueba',
      estado: 'Activo',
    });
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne((r) => r.url === '/api/universidades/72/programas').flush(pagina);
    http.expectOne('/api/universidades').flush([{ id: 72, nombre: 'Universidad de prueba' }]);
    http
      .expectOne('/api/universidades/72')
      .flush({ id: 72, nombre: 'Universidad de prueba', estado: 'Activo' });
    http.verify();
  });
});
