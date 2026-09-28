import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ListadoUniversidades } from './listado-universidades';

describe('Universidades conectado', () => {
  const universidad = {
    id: 55,
    nombre: 'Universidad prueba',
    pais: 'Colombia',
    perfil: null,
    estado: 'Activo',
  };
  function iniciar() {
    TestBed.configureTestingModule({
      imports: [ListadoUniversidades],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const fixture = TestBed.createComponent(ListadoUniversidades);
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne((r) => r.url === '/api/universidades/buscar')
      .flush({
        contenido: [universidad],
        totalElementos: 1,
        totalPaginas: 1,
        pagina: 1,
        tamano: 10,
      });
    fixture.detectChanges();
    return { fixture, http };
  }
  afterEach(() => TestBed.inject(HttpTestingController).verify());
  it('enlaza los programas por ID y no muestra importación', () => {
    const { fixture } = iniciar();
    expect(
      fixture.nativeElement
        .querySelector('a[aria-label="Ver programas de Universidad prueba"]')
        .getAttribute('href'),
    ).toBe('/admin/universidades/55/programas');
    expect(fixture.nativeElement.textContent).not.toContain('Importar');
  });
  it('crea mediante HTTP, valida obligatorios y vuelve a consultar', async () => {
    const { fixture, http } = iniciar();
    const botones = () =>
      Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    botones()
      .find((b) => b.textContent?.includes('Agregar universidad'))!
      .click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(botones().find((b) => b.textContent?.includes('Guardar universidad'))!.disabled).toBe(
      true,
    );
    for (const [id, value] of [
      ['nombre-universidad', 'Nueva universidad'],
      ['pais-universidad', 'Colombia'],
    ]) {
      const input = fixture.nativeElement.querySelector('#' + id) as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    const post = http.expectOne((r) => r.url === '/api/universidades' && r.method === 'POST');
    expect(post.request.body).toEqual({
      nombre: 'Nueva universidad',
      pais: 'Colombia',
      perfil: null,
    });
    post.flush({ ...universidad, nombre: 'Nueva universidad' });
    await Promise.resolve();
    await Promise.resolve();
    http
      .expectOne((r) => r.url === '/api/universidades/buscar')
      .flush({ contenido: [], totalElementos: 0, totalPaginas: 1, pagina: 1, tamano: 10 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Universidad creada correctamente.');
  });
  it('cambia el estado sin diálogo y conserva la fila si el backend falla', async () => {
    const { fixture, http } = iniciar();
    fixture.nativeElement.querySelector('[aria-label="Desactivar Universidad prueba"]').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
    const req = http.expectOne(
      (r) => r.url === '/api/universidades/55/estado' && r.method === 'PATCH',
    );
    expect(req.request.body).toEqual({ estado: 'Inactivo' });
    req.flush(
      { mensaje: 'No se pudo guardar el estado.' },
      { status: 503, statusText: 'Unavailable' },
    );
    await Promise.resolve();
    await Promise.resolve();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se pudo guardar el estado.');
    expect(fixture.nativeElement.querySelector('.estado-directo').textContent.trim()).toBe(
      'Activo',
    );
  });
  it('muestra Asociados separado y solo elimina al confirmar la papelera', async () => {
    const { fixture, http } = iniciar();
    expect(fixture.nativeElement.querySelectorAll('tbody .acciones-registro button').length).toBe(
      3,
    );
    expect(fixture.nativeElement.querySelector('thead').textContent).toContain('Asociados');
    const c = fixture.componentInstance as any;
    c.solicitarEliminar(universidad);
    fixture.detectChanges();
    http.expectNone((r) => r.method === 'DELETE');
    expect(fixture.nativeElement.querySelector('[role="dialog"]').textContent).toContain(
      'no podrá recuperarse',
    );
    c.pendienteEliminar.set(null);
    http.expectNone((r) => r.method === 'DELETE');
    c.solicitarEliminar(universidad);
    const promesa = c.confirmarEliminar();
    http
      .expectOne((r) => r.url === '/api/universidades/55/definitivo' && r.method === 'DELETE')
      .flush(null);
    await promesa;
    http
      .expectOne((r) => r.url === '/api/universidades/buscar')
      .flush({ contenido: [], pagina: 1, totalPaginas: 1, totalElementos: 0 });
  });
});
