import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ImportarRotaciones } from './importar-rotaciones';
import { Workbook } from 'exceljs';
import { leerExcel } from '../../../../shared/excel/excel';
describe('Importación Excel de rotaciones', () => {
  it('lee fechas reales del XLSX sin fórmulas ejecutables', async () => {
    const w = new Workbook();
    const h = w.addWorksheet('Datos');
    h.addRow(['Asignatura', 'Servicio', 'Inicio', 'Fin']);
    h.addRow(['A', 'S', new Date('2091-01-01T00:00:00Z'), { formula: '1+1', result: 2 }]);
    const bytes = await w.xlsx.writeBuffer();
    const file = {
      name: 'prueba.xlsx',
      size: bytes.byteLength,
      arrayBuffer: async () => bytes,
    } as File;
    const hojas = await leerExcel(file);
    expect(hojas[0].filas[1][2]).toBe('2091-01-01');
    expect(hojas[0].filas[1][3]).toContain('FÓRMULA');
  });
  it('mapea columnas, valida sin guardar y exige confirmar la importación', async () => {
    TestBed.configureTestingModule({
      imports: [ImportarRotaciones],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const f = TestBed.createComponent(ImportarRotaciones);
    const c = f.componentInstance as any;
    const http = TestBed.inject(HttpTestingController);
    c.hojas.set([
      {
        nombre: 'Datos',
        filas: [
          ['Fin', 'Servicio', 'Inicio', 'Asignatura'],
          ['2091-01-31', 'S', '2091-01-01', 'A'],
        ],
      },
    ]);
    c.columnas = [3, 1, 2, 0];
    const tarea = c.validar();
    const req = http.expectOne('/api/rotaciones/gestion/importar/validar');
    expect(req.request.body.filas[0]).toEqual({
      fila: 2,
      asignatura: 'A',
      servicio: 'S',
      inicio: '2091-01-01',
      fin: '2091-01-31',
    });
    req.flush({ total: 1, VALIDO: 1, CREADO: 0, OMITIDO: 0, ERROR: 0, filas: [] });
    await tarea;
    http.expectNone('/api/rotaciones/gestion/importar');
    const guardar = c.importar();
    http
      .expectOne('/api/rotaciones/gestion/importar')
      .flush({ total: 1, VALIDO: 0, CREADO: 1, OMITIDO: 0, ERROR: 0, filas: [] });
    await guardar;
    expect(c.terminado()).toBe(true);
    http.verify();
  });
});
