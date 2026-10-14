import { DashboardDatos } from '../../../../models/dashboard.model';
import { armarReporte, describirPeriodo, nombreReporte } from './reporte';

const DATOS: DashboardDatos = {
  indicadores: [
    { id: 'con-rotacion', etiqueta: 'Con rotación', valor: '207', detalle: '97%', tono: 'teal' },
    { id: 'ocupacion', etiqueta: 'Ocupación máxima', valor: '160%', detalle: 'x', tono: 'rojo' },
  ],
  situacionPorPrograma: [{ etiqueta: 'Geriatría', conRotacion: 30, enEspera: 1 }],
  cargaResidentes: [{ etiqueta: '0 rotaciones', valor: 7 }],
  mapaOcupacion: {
    meses: ['2025-11', '2025-12'],
    filas: [
      {
        servicio: 'Alto Riesgo',
        cupo: 50,
        diasExceso: 30,
        maximo: 160,
        celdas: [
          { mes: '2025-11', pico: 20, porcentaje: 40 },
          { mes: '2025-12', pico: 80, porcentaje: 160 },
        ],
      },
    ],
  },
  rotacionesPorServicio: [
    { etiqueta: 'Cirugía', valor: 4, maximo: 9 },
    { etiqueta: 'Alto Riesgo', valor: 9, maximo: 9 },
  ],
  distribucionUniversidad: [{ etiqueta: 'Javeriana', porcentaje: 100 }],
  tendenciaInicios: [{ etiqueta: '2025-08', valor: 3 }],
  alertas: [],
  revisar: [
    { residenteId: 1, residente: 'Ana', programa: 'Geriatría', motivo: 'cruce', detalle: 'x' },
  ],
  totalRegistros: 3,
};
const CORTE = {
  periodo: '2025-2',
  universidad: 'Todas',
  programa: 'Especialización en Geriatría',
  servicio: 'Todos',
  asignatura: 'Cadera · Alto Riesgo',
  asignaturaId: '7',
};

describe('reporte del dashboard', () => {
  const hojas = armarReporte(DATOS, CORTE, new Date(2025, 9, 15, 8, 5));
  const hoja = (nombre: string) => hojas.find((h) => h.nombre === nombre)!;

  it('explica el periodo con sus meses', () => {
    expect(describirPeriodo('2025-1')).toBe('enero a junio de 2025');
    expect(describirPeriodo('2025-2')).toBe('julio a diciembre de 2025');
  });

  it('nombra el archivo con el periodo', () => {
    expect(nombreReporte(CORTE)).toBe('reporte-dashboard-2025-2.xlsx');
  });

  it('resume filtros, indicadores y avisos; las cifras enteras quedan como número', () => {
    const filas = hoja('Resumen').filas;
    expect(filas[0]).toEqual(['Periodo', '2025-2', 'julio a diciembre de 2025']);
    expect(filas[2]).toEqual(['Programa', 'Especialización en Geriatría', '']);
    expect(filas[4]).toEqual(['Asignatura', 'Cadera · Alto Riesgo', '']);
    expect(filas[5]).toEqual(['Generado el', '2025-10-15 08:05', '']);
    expect(filas).toContainEqual(['Con rotación', 207, '97%']);
    expect(filas).toContainEqual(['Ocupación máxima', '160%', 'x']);
    expect(filas.at(-1)).toEqual(['Sin pendientes registrados', '', '']);
  });

  it('incluye totales por programa, el motivo legible y el pico de cada mes', () => {
    expect(hoja('Situación por programa').filas).toEqual([['Geriatría', 30, 1, 31]]);
    expect(hoja('Residentes a revisar').filas).toEqual([
      ['Ana', 'Geriatría', 'Cruce de fechas', 'x'],
    ]);
    const ocupacion = hoja('Ocupación por servicio');
    expect(ocupacion.columnas).toContain('Pico 2025-12');
    expect(ocupacion.filas).toEqual([['Alto Riesgo', 50, 20, 80, 160, 30]]);
  });

  it('ordena los servicios de mayor a menor y omite el reparto con una sola universidad', () => {
    expect(hoja('Rotaciones por servicio').filas).toEqual([
      ['Alto Riesgo', 9],
      ['Cirugía', 4],
    ]);
    expect(hojas.map((h) => h.nombre)).not.toContain('Distribución por universidad');
    expect(hojas.every((h) => h.nombre.length <= 31)).toBe(true);
  });
});
