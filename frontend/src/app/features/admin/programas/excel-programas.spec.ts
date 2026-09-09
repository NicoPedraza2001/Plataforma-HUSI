import { letraColumna, mapearFilas } from './excel-programas';
describe('Mapeo de Excel', () => {
  it('respeta columnas elegidas, encabezados desplazados y números originales de fila', () => {
    const hoja = {
      nombre: 'Datos',
      filas: [
        ['Título'],
        ['Institución', 'Ignorar', 'Programa'],
        ['Universidad A', 'x', 'Pediatría'],
        ['', '', ''],
        ['Universidad B', 'x', ''],
      ],
    };
    expect(mapearFilas(hoja, 2, 2, 0)).toEqual([
      { fila: 3, nombre: 'Pediatría', universidad: 'Universidad A' },
      { fila: 5, nombre: '', universidad: 'Universidad B' },
    ]);
  });
  it('identifica columnas después de Z', () => {
    expect(letraColumna(26)).toBe('AA');
    expect(letraColumna(51)).toBe('AZ');
  });
});
