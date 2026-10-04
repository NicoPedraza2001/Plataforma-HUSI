import { crearFilaDePrueba } from './datos-de-prueba';
import {
  actualizarFilas,
  buscarSugerencias,
  buscarValoresOriginales,
  calcularSimilitud,
  contar,
  describirFilas,
  esMismoTexto,
  excluirFilas,
  indexarPorNumeroDeFila,
  valoresDistintos,
} from './emparejamiento';
import { buscarPasoParaCorregir } from './pasos-importacion';

describe('emparejamiento', () => {
  it('compara como el backend: sin mayúsculas ni espacios extremos, pero con tildes', () => {
    expect(esMismoTexto('  Javeriana ', 'JAVERIANA')).toBe(true);
    expect(esMismoTexto('Pediatría', 'Pediatria')).toBe(false);
  });

  it('mide la similitud tolerando tildes, abreviaturas y errores de digitación', () => {
    expect(calcularSimilitud('Pediatría', 'pediatria')).toBe(1);
    expect(
      calcularSimilitud('Univ. Javeriana', 'Pontificia Universidad Javeriana'),
    ).toBeGreaterThan(0.5);
    expect(calcularSimilitud('Javeriana', 'Rosario')).toBeLessThan(0.3);
    expect(
      calcularSimilitud('Universidad Inventada de Prueba', 'Pontificia Universidad Javeriana'),
    ).toBeLessThan(0.45);
  });

  it('sugiere los registros parecidos del más al menos similar', () => {
    const registros = [
      { nombre: 'Universidad del Rosario' },
      { nombre: 'Pontificia Universidad Javeriana' },
    ];
    expect(buscarSugerencias('Javeriana', registros).map((registro) => registro.nombre)).toEqual([
      'Pontificia Universidad Javeriana',
    ]);
  });

  it('reescribe y excluye solo las filas indicadas', () => {
    const filas = [crearFilaDePrueba({ fila: 2 }), crearFilaDePrueba({ fila: 3 })];
    const actualizadas = actualizarFilas(filas, [3], { universidad: 'Otra' });
    expect(actualizadas.map((fila) => fila.universidad)).toEqual([
      'Pontificia Universidad Javeriana',
      'Otra',
    ]);
    expect(excluirFilas(filas, [2]).map((fila) => fila.fila)).toEqual([3]);
  });

  it('informa cómo venía escrito el valor en el archivo original', () => {
    const originales = indexarPorNumeroDeFila([crearFilaDePrueba({ fila: 2, universidad: 'PUJ' })]);
    expect(buscarValoresOriginales(originales, [2], 'universidad', 'Javeriana')).toEqual(['PUJ']);
    expect(
      valoresDistintos([crearFilaDePrueba(), crearFilaDePrueba()], 'universidad'),
    ).toHaveLength(1);
  });

  it('concuerda la palabra con la cantidad', () => {
    expect(contar(1, 'fila', 'filas')).toBe('1 fila');
    expect(contar(3, 'fila', 'filas')).toBe('3 filas');
  });

  it('resume listas largas de filas', () => {
    expect(describirFilas([2, 3, 4, 5, 6, 7, 8])).toBe('2, 3, 4, 5, 6 y 2 más');
  });
});

describe('buscarPasoParaCorregir', () => {
  it('relaciona los mensajes del backend con el paso donde se corrigen', () => {
    expect(buscarPasoParaCorregir('El programa del residente no coincide')).toBe('residentes');
    expect(buscarPasoParaCorregir('Indica el país para crear la universidad.')).toBe(
      'universidades',
    );
    expect(buscarPasoParaCorregir('Falta programa.')).toBe('programas');
    expect(buscarPasoParaCorregir('El cupo máximo no coincide con el registro existente.')).toBe(
      'servicios',
    );
    expect(
      buscarPasoParaCorregir('No existe asignatura. Regístralo o habilita crear datos faltantes.'),
    ).toBe('asignaturas');
    expect(buscarPasoParaCorregir('Fechas inválidas. Usa celdas de fecha o AAAA-MM-DD.')).toBe(
      'rotaciones',
    );
    expect(buscarPasoParaCorregir('Algo inesperado')).toBeNull();
  });
});
