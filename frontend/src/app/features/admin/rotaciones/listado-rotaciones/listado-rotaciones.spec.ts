import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ListadoRotaciones } from './listado-rotaciones';
describe('Rotaciones conectadas', () => {
  const fila = {
    asignaturaId: 4,
    inicio: '2091-01-01',
    fin: '2091-01-31',
    asignatura: 'Prueba',
    servicio: 'Servicio',
    servicioId: 3,
    activa: true,
    disponibilidad: 'Disponible',
    cupoDisponible: 3,
    relaciones: 0,
  };
  function crear() {
    TestBed.configureTestingModule({
      imports: [ListadoRotaciones],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const f = TestBed.createComponent(ListadoRotaciones);
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne((r) => r.url === '/api/rotaciones/gestion')
      .flush({ contenido: [fila], pagina: 1, totalPaginas: 1, totalElementos: 1, tamano: 10 });
    http.expectOne('/api/rotaciones/gestion/catalogos').flush({ asignaturas: [], servicios: [] });
    f.detectChanges();
    return { f, http };
  }
  it('cancelar una eliminación no envía DELETE; confirmar sí', async () => {
    const { f, http } = crear();
    const c = f.componentInstance as any;
    c.solicitar(fila, true);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('no podrá recuperarse');
    const cancelar = [...f.nativeElement.querySelectorAll('button')].find(
      (b: any) => b.textContent.trim() === 'Cancelar',
    ) as HTMLButtonElement;
    cancelar.click();
    http.expectNone((r) => r.method === 'DELETE');
    expect(c.pendiente()).toBeNull();
    c.solicitar(fila, true);
    const tarea = c.confirmar();
    http
      .expectOne(
        (r) => r.method === 'DELETE' && r.url === '/api/rotaciones/gestion/4/2091-01-01/2091-01-31',
      )
      .flush(null);
    await tarea;
    http
      .expectOne((r) => r.url === '/api/rotaciones/gestion')
      .flush({ contenido: [], pagina: 1, totalPaginas: 1, totalElementos: 0 });
    http.verify();
  });
  it('cambiar de página consulta al backend', () => {
    const { f, http } = crear();
    (f.componentInstance as any).cargar(2);
    http
      .expectOne((r) => r.url === '/api/rotaciones/gestion' && r.params.get('pagina') === '2')
      .flush({ contenido: [], pagina: 2, totalPaginas: 2, totalElementos: 11 });
    http.verify();
  });
  it('muestra un error del servidor sin romper el listado', () => {
    const { f, http } = crear();
    (f.componentInstance as any).cargar();
    http
      .expectOne((r) => r.url === '/api/rotaciones/gestion')
      .flush(
        { mensaje: 'Base temporalmente no disponible' },
        { status: 503, statusText: 'Unavailable' },
      );
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Reintentar');
    http.verify();
  });
  it('cambia estado en la fila sin abrir confirmación', async () => {
    const { f, http } = crear(),
      c = f.componentInstance as any;
    const tarea = c.cambiarEstado(fila);
    expect(f.nativeElement.querySelector('[role="dialog"]')).toBeNull();
    http
      .expectOne((r) => r.method === 'PATCH' && r.url.endsWith('/estado'))
      .flush({ ...fila, activa: false });
    await tarea;
    f.detectChanges();
    http
      .expectOne((r) => r.url === '/api/rotaciones/gestion')
      .flush({
        contenido: [{ ...fila, activa: false }],
        pagina: 1,
        totalPaginas: 1,
        totalElementos: 1,
        tamano: 10,
      });
    f.detectChanges();
    expect(f.nativeElement.querySelector('.estado-directo').textContent.trim()).toBe('Inactivo');
    expect(f.nativeElement.querySelectorAll('thead th').length).toBe(5);
    expect(f.nativeElement.querySelectorAll('tbody .acciones-registro button').length).toBe(3);
    http.verify();
  });
  it('al crear una rotación con residentes en espera abre su listado para trasladarlos', async () => {
    const { f, http } = crear();
    const c = f.componentInstance as any;
    c.asignaturaId = 4;
    c.inicio = '2091-01-01';
    c.fin = '2091-01-31';
    const tarea = c.guardar();
    http.expectOne((r) => r.method === 'POST' && r.url === '/api/rotaciones/gestion').flush(fila);
    await new Promise((r) => setTimeout(r));
    http
      .expectOne((r) => r.url === '/api/rotaciones/gestion')
      .flush({ contenido: [fila], pagina: 1, totalPaginas: 1, totalElementos: 1, tamano: 10 });
    http
      .expectOne('/api/rotaciones/gestion/4/2091-01-01/2091-01-31/espera')
      .flush([{ residenteId: 9 }]);
    await tarea;
    expect(c.rotacionResidentes()).toEqual(fila);
    expect(c.aviso()).toContain('esperando cupo');
  });
  it('muestra las fechas en formato día/mes/año', () => {
    const { f } = crear();
    const texto = f.nativeElement.textContent;
    expect(texto).toContain('01/01/2091');
    expect(texto).toContain('31/01/2091');
    expect(texto).not.toContain('2091-01-01');
  });
  it('una rotación finalizada solo ofrece consultar sus residentes', () => {
    TestBed.configureTestingModule({
      imports: [ListadoRotaciones],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const f = TestBed.createComponent(ListadoRotaciones);
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne((r) => r.url === '/api/rotaciones/gestion')
      .flush({
        contenido: [{ ...fila, finalizada: true }],
        pagina: 1,
        totalPaginas: 1,
        totalElementos: 1,
        tamano: 10,
      });
    http.expectOne('/api/rotaciones/gestion/catalogos').flush({ asignaturas: [], servicios: [] });
    f.detectChanges();
    const fila0 = f.nativeElement.querySelector('tbody tr') as HTMLElement;
    expect(fila0.textContent).toContain('Finalizada');
    expect(fila0.textContent).toContain('Ver residentes');
    expect(fila0.textContent).not.toContain('Editar');
    expect(fila0.querySelector('.papelera')).toBeNull();
  });
});
