import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ListadoProgramas } from './listado-programas';
describe('Acciones de programas', () => {
  const programa = {
    id: 91,
    nombre: 'Prueba',
    universidadId: 1,
    universidad: 'U',
    estado: 'Activo',
  };
  function iniciar() {
    TestBed.configureTestingModule({
      imports: [ListadoProgramas],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const fixture = TestBed.createComponent(ListadoProgramas),
      http = TestBed.inject(HttpTestingController);
    http
      .expectOne((peticion) => peticion.url === '/api/programas/buscar')
      .flush({ contenido: [programa], pagina: 1, totalPaginas: 1, totalElementos: 1 });
    http.expectOne('/api/universidades').flush([{ id: 1, nombre: 'U' }]);
    fixture.detectChanges();
    return { fixture, http };
  }
  it('actualiza el estado desde la respuesta y permite reactivar sin popup', async () => {
    const { fixture, http } = iniciar(),
      componente = fixture.componentInstance as any;
    expect(fixture.nativeElement.querySelectorAll('tbody .acciones-registro button').length).toBe(
      3,
    );
    let tarea = componente.cambiarEstado(programa);
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
    http
      .expectOne((peticion) => peticion.url === '/api/programas/91/estado')
      .flush({ ...programa, estado: 'Inactivo' });
    await tarea;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.estado-directo').textContent.trim()).toBe(
      'Inactivo',
    );
    tarea = componente.cambiarEstado({ ...programa, estado: 'Inactivo' });
    const req = http.expectOne('/api/programas/91/estado');
    expect(req.request.body.estado).toBe('Activo');
    req.flush(programa);
    await tarea;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.estado-directo').textContent.trim()).toBe(
      'Activo',
    );
    http.verify();
  });
  it('la papelera usa borrado definitivo únicamente tras confirmar', async () => {
    const { fixture, http } = iniciar(),
      componente = fixture.componentInstance as any;
    componente.solicitarEliminar(programa);
    fixture.detectChanges();
    http.expectNone((peticion) => peticion.method === 'DELETE');
    expect(fixture.nativeElement.querySelector('[role="dialog"]').textContent).toContain(
      'no podrá recuperarse',
    );
    const tarea = componente.confirmarEliminar();
    http
      .expectOne(
        (peticion) =>
          peticion.url === '/api/programas/91/definitivo' && peticion.method === 'DELETE',
      )
      .flush(null);
    await tarea;
    http
      .expectOne((peticion) => peticion.url === '/api/programas/buscar')
      .flush({ contenido: [], pagina: 1, totalPaginas: 1, totalElementos: 0 });
    http.expectOne('/api/universidades').flush([]);
    http.verify();
  });
});
