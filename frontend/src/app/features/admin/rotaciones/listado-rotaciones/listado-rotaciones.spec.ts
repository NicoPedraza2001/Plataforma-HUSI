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
    const fixture = TestBed.createComponent(ListadoRotaciones);
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne((peticion) => peticion.url === '/api/rotaciones/gestion')
      .flush({ contenido: [fila], pagina: 1, totalPaginas: 1, totalElementos: 1, tamano: 10 });
    http.expectOne('/api/rotaciones/gestion/catalogos').flush({ asignaturas: [], servicios: [] });
    fixture.detectChanges();
    return { fixture, http };
  }
  it('cancelar una eliminación no envía DELETE; confirmar sí', async () => {
    const { fixture, http } = crear();
    const componente = fixture.componentInstance as any;
    componente.solicitarEliminar(fila, true);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('no podrá recuperarse');
    const cancelar = [...fixture.nativeElement.querySelectorAll('button')].find(
      (boton: any) => boton.textContent.trim() === 'Cancelar',
    ) as HTMLButtonElement;
    cancelar.click();
    http.expectNone((peticion) => peticion.method === 'DELETE');
    expect(componente.accionPendiente()).toBeNull();
    componente.solicitarEliminar(fila, true);
    const tarea = componente.confirmar();
    http
      .expectOne(
        (peticion) =>
          peticion.method === 'DELETE' &&
          peticion.url === '/api/rotaciones/gestion/4/2091-01-01/2091-01-31',
      )
      .flush(null);
    await tarea;
    http
      .expectOne((peticion) => peticion.url === '/api/rotaciones/gestion')
      .flush({ contenido: [], pagina: 1, totalPaginas: 1, totalElementos: 0 });
    http.verify();
  });
  it('muestra inicio y fin en columnas y filtra por rango de fechas', () => {
    const { fixture, http } = crear();
    const encabezados = [...fixture.nativeElement.querySelectorAll('th')].map((celda: any) =>
      celda.textContent.trim(),
    );
    expect(encabezados).toContain('Fecha de inicio');
    expect(encabezados).toContain('Fecha de fin');
    expect(fixture.nativeElement.textContent).toContain('31/01/2091');
    const componente = fixture.componentInstance as any;
    componente.filtroDesde = '2091-01-15';
    componente.filtroHasta = '2091-02-15';
    componente.filtrarPorFechas();
    http
      .expectOne(
        (peticion) =>
          peticion.url === '/api/rotaciones/gestion' &&
          peticion.params.get('desde') === '2091-01-15' &&
          peticion.params.get('hasta') === '2091-02-15',
      )
      .flush({ contenido: [fila], pagina: 1, totalPaginas: 1, totalElementos: 1 });
    componente.filtroHasta = '2091-01-01';
    componente.filtrarPorFechas();
    http.expectNone((peticion) => peticion.url === '/api/rotaciones/gestion');
    expect(componente.errorFiltroFechas()).toContain('no puede ser anterior');
    http.verify();
  });
  it('cambiar de página consulta al backend', () => {
    const { fixture, http } = crear();
    (fixture.componentInstance as any).cargar(2);
    http
      .expectOne(
        (peticion) =>
          peticion.url === '/api/rotaciones/gestion' && peticion.params.get('pagina') === '2',
      )
      .flush({ contenido: [], pagina: 2, totalPaginas: 2, totalElementos: 11 });
    http.verify();
  });
  it('muestra un error del servidor sin romper el listado', () => {
    const { fixture, http } = crear();
    (fixture.componentInstance as any).cargar();
    http
      .expectOne((peticion) => peticion.url === '/api/rotaciones/gestion')
      .flush(
        { mensaje: 'Base temporalmente no disponible' },
        { status: 503, statusText: 'Unavailable' },
      );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Reintentar');
    http.verify();
  });
  it('cambia estado en la fila sin abrir confirmación', async () => {
    const { fixture, http } = crear(),
      componente = fixture.componentInstance as any;
    const tarea = componente.cambiarEstado(fila);
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
    http
      .expectOne((peticion) => peticion.method === 'PATCH' && peticion.url.endsWith('/estado'))
      .flush({ ...fila, activa: false });
    await tarea;
    fixture.detectChanges();
    http
      .expectOne((peticion) => peticion.url === '/api/rotaciones/gestion')
      .flush({
        contenido: [{ ...fila, activa: false }],
        pagina: 1,
        totalPaginas: 1,
        totalElementos: 1,
        tamano: 10,
      });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.estado-directo').textContent.trim()).toBe(
      'Inactivo',
    );
    expect(fixture.nativeElement.querySelectorAll('thead th').length).toBe(6);
    expect(fixture.nativeElement.querySelectorAll('tbody .acciones-registro button').length).toBe(
      3,
    );
    http.verify();
  });
  it('al crear una rotación con residentes en espera abre su listado para trasladarlos', async () => {
    const { fixture, http } = crear();
    const componente = fixture.componentInstance as any;
    componente.asignaturaId = 4;
    componente.inicio = '2091-01-01';
    componente.fin = '2091-01-31';
    const tarea = componente.guardar();
    http
      .expectOne(
        (peticion) => peticion.method === 'POST' && peticion.url === '/api/rotaciones/gestion',
      )
      .flush(fila);
    await new Promise((peticion) => setTimeout(peticion));
    http
      .expectOne((peticion) => peticion.url === '/api/rotaciones/gestion')
      .flush({ contenido: [fila], pagina: 1, totalPaginas: 1, totalElementos: 1, tamano: 10 });
    http
      .expectOne('/api/rotaciones/gestion/4/2091-01-01/2091-01-31/espera')
      .flush([{ residenteId: 9 }]);
    await tarea;
    expect(componente.rotacionVerResidentes()).toEqual(fila);
    expect(componente.aviso()).toContain('esperando cupo');
  });
  it('muestra las fechas en formato día/mes/año', () => {
    const { fixture } = crear();
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('01/01/2091');
    expect(texto).toContain('31/01/2091');
    expect(texto).not.toContain('2091-01-01');
  });
  it('una rotación finalizada solo ofrece consultar sus residentes', () => {
    TestBed.configureTestingModule({
      imports: [ListadoRotaciones],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(ListadoRotaciones);
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne((peticion) => peticion.url === '/api/rotaciones/gestion')
      .flush({
        contenido: [{ ...fila, finalizada: true }],
        pagina: 1,
        totalPaginas: 1,
        totalElementos: 1,
        tamano: 10,
      });
    http.expectOne('/api/rotaciones/gestion/catalogos').flush({ asignaturas: [], servicios: [] });
    fixture.detectChanges();
    const fila0 = fixture.nativeElement.querySelector('tbody tr') as HTMLElement;
    expect(fila0.textContent).toContain('Finalizada');
    expect(fila0.textContent).toContain('Ver residentes');
    expect(fila0.textContent).not.toContain('Editar');
    expect(fila0.querySelector('.papelera')).toBeNull();
  });
});
