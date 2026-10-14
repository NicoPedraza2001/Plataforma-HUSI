import { signal } from '@angular/core';
import { Catalogos } from '../../../../models/catalogos.model';
import {
  elegirPeriodoInicial,
  FiltrosDashboard,
  nombreCorto,
  periodoActual,
} from './filtros-dashboard';

const CATALOGOS = {
  periodos: ['2025-2', '2025-1'],
  universidades: ['Javeriana'],
  programas: ['Geriatría', 'Medicina Interna'],
  servicios: ['Alto Riesgo', 'Cirugía'],
} as Catalogos;
const ASIGNATURAS = [
  { id: 7, nombre: 'Cadera', servicio: 'Alto Riesgo' },
  { id: 8, nombre: 'Cirugía oculoplástica', servicio: 'Cirugía' },
];
// Geriatría solo rota en Alto Riesgo; Medicina Interna, en Cirugía.
const RELACIONES = [
  { programa: 'Geriatría', servicio: 'Alto Riesgo', asignaturaId: 7 },
  { programa: 'Medicina Interna', servicio: 'Cirugía', asignaturaId: 8 },
];

describe('FiltrosDashboard', () => {
  let filtros: FiltrosDashboard;

  beforeEach(() => {
    filtros = new FiltrosDashboard({
      catalogos: signal(CATALOGOS),
      asignaturas: signal(ASIGNATURAS),
      relaciones: signal(RELACIONES),
    });
    filtros.iniciar(CATALOGOS.periodos);
  });

  it('abre en el periodo más reciente con datos si el actual no tiene', () => {
    expect(filtros.periodo()).toBe('2025-2');
    expect(filtros.parametros()).toEqual({ periodo: '2025-2' });
    expect(filtros.hayFiltros()).toBe(false);
  });

  it('acota servicios por programa y programas por servicio', () => {
    filtros.elegir('programa', 'Geriatría');
    expect(filtros.servicios()).toEqual(['Todos', 'Alto Riesgo']);

    filtros.elegir('programa', 'Todos');
    filtros.elegir('servicio', 'Cirugía');
    expect(filtros.programas()).toEqual(['Todos', 'Medicina Interna']);
  });

  it('muestra el servicio de cada asignatura solo cuando no hay servicio elegido', () => {
    expect(filtros.asignaturas().map((opcion) => opcion.etiqueta)).toEqual([
      'Cadera · Alto Riesgo',
      'Cirugía oculoplástica · Cirugía',
    ]);
    filtros.elegir('servicio', 'Alto Riesgo');
    expect(filtros.asignaturas().map((opcion) => opcion.etiqueta)).toEqual(['Cadera']);
  });

  it('suelta un filtro que deja de estar entre las opciones', () => {
    filtros.elegir('asignatura', '7');
    filtros.elegir('servicio', 'Cirugía');
    expect(filtros.asignatura()).toBe('Todas');
  });

  it('arma los parámetros y el corte con lo elegido, y limpiar vuelve al inicio', () => {
    filtros.elegir('servicio', 'Cirugía');
    filtros.elegir('asignatura', '8');
    filtros.elegir('programa', 'Medicina Interna');

    expect(filtros.parametros()).toEqual({
      periodo: '2025-2',
      programa: 'Medicina Interna',
      servicio: 'Cirugía',
      asignaturaId: '8',
    });
    expect(filtros.corte().asignatura).toBe('Cirugía oculoplástica');

    filtros.limpiar();
    expect(filtros.parametros()).toEqual({ periodo: '2025-2' });
    expect(filtros.hayFiltros()).toBe(false);
  });
});

describe('utilidades de filtros', () => {
  it('quita el prefijo común de los programas', () => {
    expect(nombreCorto('Especialización en Geriatría')).toBe('Geriatría');
  });

  it('calcula el semestre: 1 de enero a junio y 2 de julio a diciembre', () => {
    expect(periodoActual(new Date(2026, 5, 30))).toBe('2026-1');
    expect(periodoActual(new Date(2026, 6, 1))).toBe('2026-2');
  });

  it('elige el periodo inicial', () => {
    expect(elegirPeriodoInicial(['2026-2', '2026-1'], '2026-2')).toBe('2026-2');
    expect(elegirPeriodoInicial(['2026-1', '2025-2'], '2026-2')).toBe('2026-1');
    expect(elegirPeriodoInicial(['2027-1'], '2026-2')).toBe('2027-1');
    expect(elegirPeriodoInicial([], '2026-2')).toBe('2026-2');
  });
});
