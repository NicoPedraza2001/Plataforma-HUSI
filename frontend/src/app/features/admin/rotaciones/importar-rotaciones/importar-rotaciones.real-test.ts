// Prueba optativa contra el backend local real. No forma parte de los *.spec.ts.
// Ejecutar: ng test --watch=false --include=src/app/features/admin/rotaciones/importar-rotaciones.real-test.ts
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ImportarRotaciones } from './importar-rotaciones';
import { Workbook } from 'exceljs';
describe('XLSX → componente Angular → HTTP real → PostgreSQL', () => {
  it('valida, importa, detecta duplicados y permite quitar el archivo', async () => {
    const base = 'http://localhost:8080/api/rotaciones/gestion';
    const catalogo = await (await fetch(base + '/catalogos')).json();
    const a = catalogo.asignaturas[0];
    expect(a).toBeTruthy();
    const inicio = '2094-09-14',
      fin = '2094-09-15',
      ruta = `${base}/${a.id}/${inicio}/${fin}`;
    const antes = await fetch(ruta);
    expect(antes.status).toBe(404); // Nunca tocar un registro previo.
    TestBed.configureTestingModule({
      imports: [ImportarRotaciones],
      providers: [
        provideHttpClient(
          withFetch(),
          withInterceptors([
            (req, next) => next(req.clone({ url: 'http://localhost:8080' + req.url })),
          ]),
        ),
      ],
    });
    const f = TestBed.createComponent(ImportarRotaciones),
      c = f.componentInstance as any;
    const libro = new Workbook();
    const hoja = libro.addWorksheet('Carga');
    hoja.addRow(['Asignatura', 'Servicio', 'Fecha de inicio', 'Fecha de fin']);
    hoja.addRow([
      a.nombre,
      a.servicio,
      new Date(inicio + 'T00:00:00Z'),
      new Date(fin + 'T00:00:00Z'),
    ]);
    hoja.addRow([a.nombre, a.servicio, inicio, fin]);
    hoja.addRow([a.nombre, a.servicio, 'fecha incorrecta', fin]);
    const bytes = await libro.xlsx.writeBuffer();
    await c.cargar({
      name: 'prueba-rotaciones.xlsx',
      size: bytes.byteLength,
      arrayBuffer: async () => bytes,
    } as File);
    expect(c.columnas).toEqual([0, 1, 2, 3]);
    await c.validar();
    expect(c.error()).toBe('');
    expect(c.resultado().VALIDO).toBe(1);
    expect(c.resultado().OMITIDO).toBe(1);
    expect(c.resultado().ERROR).toBe(1);
    expect((await fetch(ruta)).status).toBe(404);
    try {
      await c.importar();
      expect(c.error()).toBe('');
      expect(c.resultado().CREADO).toBe(1);
      expect(c.terminado()).toBe(true);
      const guardada = await (await fetch(ruta)).json();
      expect(guardada.asignaturaId).toBe(a.id);
      expect(guardada.activa).toBe(true);
      c.terminado.set(false);
      await c.validar();
      expect(c.resultado().VALIDO).toBe(0);
      expect(c.resultado().OMITIDO).toBe(2);
      c.reiniciar();
      expect(c.hojas()).toEqual([]);
      expect(c.archivo()).toBe('');
      expect(c.resultado()).toBeNull();
    } finally {
      const eliminado = await fetch(ruta, { method: 'DELETE' });
      expect(eliminado.status).toBe(204);
    }
    expect((await fetch(ruta)).status).toBe(404);
  }, 30000);
});
