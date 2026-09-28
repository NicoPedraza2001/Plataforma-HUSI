import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ImportarRotaciones } from './importar-rotaciones';
import { CAMPOS_CARGA, detectarColumnas, prepararCarga } from './campos-carga';
describe('Carga inicial guiada', () => {
  const resultado = {
    validacion: 'huella',
    guardado: false,
    total: 1,
    validos: 1,
    advertencias: 0,
    errores: 0,
    omitidos: 0,
    nuevos: { rotacion: 1 },
    filas: [{ fila: 2, estado: 'VALIDO', mensaje: 'Correcto' }],
  };
  function crear() {
    TestBed.configureTestingModule({
      imports: [ImportarRotaciones],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(ImportarRotaciones),
      c = fixture.componentInstance as any;
    c.hojas.set([
      {
        nombre: 'Datos',
        filas: [
          CAMPOS_CARGA.map((c) => c.nombre),
          [
            'Universidad',
            'Colombia',
            'Programa',
            'Residente',
            'CC',
            '001',
            '',
            'Servicio',
            '1',
            '1',
            'Asignatura',
            '2099-01-01',
            '2099-01-31',
          ],
        ],
      },
    ]);
    c.archivo.set('datos.xlsx');
    c.detectar();
    c.preparar();
    fixture.detectChanges();
    return { c, fixture, http: TestBed.inject(HttpTestingController) };
  }
  it('detecta encabezados del archivo real y no confunde TIPO con tipo de documento', () => {
    const columnas = detectarColumnas([
      'Name',
      'DOCUMENTO2',
      'Programa',
      'Servicio_HUSI',
      'Rotaciones /asignaturas',
      'Fecha inicial',
      'Fecha final',
      'TIPO',
    ]);
    expect(columnas[3]).toBe(0);
    expect(columnas[4]).toBe(-1);
    expect(columnas[5]).toBe(1);
    expect(columnas[10]).toBe(4);
  });
  it('omite filas vacías aunque haya valores comunes', () => {
    const cols = CAMPOS_CARGA.map(() => -1);
    cols[3] = 0;
    expect(
      prepararCarga(
        [['Name'], ['Ana'], ['']],
        1,
        cols,
        CAMPOS_CARGA.map(() => 'común'),
      ),
    ).toHaveLength(1);
  });
  it('conserva correcciones al retroceder e invalida al cambiar columnas', () => {
    const { c, http } = crear();
    c.cambiarFilas([{ ...c.filas()[0], residente: 'Corregido' }]);
    c.ir(1);
    c.preparar();
    expect(c.filas()[0].residente).toBe('Corregido');
    c.resultado.set(resultado);
    c.invalidar(true);
    expect(c.disponible(4)).toBe(false);
    c.preparar();
    expect(c.filas()[0].residente).toBe('Residente');
    http.verify();
  });
  it('valida sin guardar y exige el paso de confirmación', async () => {
    const { c, http } = crear();
    const tarea = c.validar();
    const req = http.expectOne('/api/rotaciones/carga-inicial/validar');
    expect(req.request.body.filas[0].documento).toBe('001');
    req.flush(resultado);
    await tarea;
    await c.importar();
    http.expectNone('/api/rotaciones/carga-inicial/confirmar');
    c.ir(4);
    const guardar = c.importar();
    const confirmacion = http.expectOne('/api/rotaciones/carga-inicial/confirmar');
    expect(confirmacion.request.body.validacion).toBe('huella');
    confirmacion.flush({ ...resultado, guardado: true });
    await guardar;
    expect(c.paso()).toBe(5);
    expect(c.disponible(1)).toBe(false);
    await c.importar();
    http.verify();
  });
  it('conserva datos ante error y obliga a revalidar', async () => {
    const { c, http } = crear();
    c.resultado.set(resultado);
    c.ir(4);
    const guardar = c.importar();
    http
      .expectOne('/api/rotaciones/carga-inicial/confirmar')
      .flush({ mensaje: 'Cupos cambiaron' }, { status: 409, statusText: 'Conflict' });
    await guardar;
    expect(c.filas()).toHaveLength(1);
    expect(c.paso()).toBe(2);
    expect(c.resultado()).toBeNull();
    expect(c.error()).toContain('Cupos cambiaron');
    http.verify();
  });
});
