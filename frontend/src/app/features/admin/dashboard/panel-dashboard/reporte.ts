import { DashboardDatos, MotivoRevision } from '../../../../models/dashboard.model';
import { HojaDescarga } from '../../../../shared/excel/excel';

/** Filtros con los que se generó el panel; encabezan el reporte. */
export interface CorteDashboard {
  periodo: string;
  universidad: string;
  programa: string;
  servicio: string;
  /** Texto de la asignatura elegida (con su servicio si hace falta) o «Todas». */
  asignatura: string;
  asignaturaId: string | null;
}

/** Texto y distintivo de cada motivo de «Residentes a revisar»: los comparten pantalla y reporte. */
export const MOTIVOS: Record<MotivoRevision, { etiqueta: string; badge: string }> = {
  cruce: { etiqueta: 'Cruce de fechas', badge: 'badge--inactivo' },
  espera: { etiqueta: 'En lista de espera', badge: 'badge--info' },
};

/** «2025-2» → «julio a diciembre de 2025»; recuerda qué meses cubre el número. */
export function describirPeriodo(periodo: string) {
  const [anio, semestre] = periodo.split('-');
  return semestre === '1' ? `enero a junio de ${anio}` : `julio a diciembre de ${anio}`;
}

export function nombreReporte(corte: CorteDashboard) {
  return `reporte-dashboard-${corte.periodo}.xlsx`;
}

/**
 * Una hoja por sección del panel, con las mismas cifras que se ven en pantalla para el corte
 * elegido. La primera hoja resume filtros, indicadores y avisos.
 */
export function armarReporte(
  datos: DashboardDatos,
  corte: CorteDashboard,
  generado = new Date(),
): HojaDescarga[] {
  const vacia = ['', '', ''];
  const hojas: HojaDescarga[] = [
    {
      nombre: 'Resumen',
      columnas: ['Concepto', 'Valor', 'Detalle'],
      filas: [
        ['Periodo', corte.periodo, describirPeriodo(corte.periodo)],
        ['Universidad', corte.universidad, ''],
        ['Programa', corte.programa, ''],
        ['Servicio', corte.servicio, ''],
        ['Asignatura', corte.asignatura, ''],
        ['Generado el', fechaHora(generado), ''],
        vacia,
        ['INDICADORES', '', ''],
        ...datos.indicadores.map((indicador) => [
          indicador.etiqueta,
          numeroSiEs(indicador.valor),
          indicador.detalle,
        ]),
        vacia,
        ['REQUIERE ATENCIÓN', '', ''],
        ...(datos.alertas.length
          ? datos.alertas.map((alerta) => [alerta.etiqueta, alerta.cantidad, alerta.detalle])
          : [['Sin pendientes registrados', '', '']]),
      ],
    },
    {
      nombre: 'Situación por programa',
      columnas: ['Programa', 'Con rotación asignada', 'Solo en lista de espera', 'Total'],
      filas: datos.situacionPorPrograma.map((programa) => [
        programa.etiqueta,
        programa.conRotacion,
        programa.enEspera,
        programa.conRotacion + programa.enEspera,
      ]),
    },
    {
      nombre: 'Residentes a revisar',
      columnas: ['Residente', 'Programa', 'Motivo', 'Detalle'],
      filas: datos.revisar.map((residente) => [
        residente.residente,
        residente.programa,
        MOTIVOS[residente.motivo].etiqueta,
        residente.detalle,
      ]),
    },
    {
      nombre: 'Ocupación por servicio',
      columnas: [
        'Servicio',
        'Cupo simultáneo',
        ...datos.mapaOcupacion.meses.map((mes) => `Pico ${mes}`),
        'Ocupación máxima (%)',
        'Días sobre el cupo',
      ],
      filas: datos.mapaOcupacion.filas.map((fila) => [
        fila.servicio,
        fila.cupo,
        ...fila.celdas.map((celda) => celda.pico),
        fila.maximo,
        fila.diasExceso,
      ]),
    },
    {
      nombre: 'Carga por residente',
      columnas: ['Rotaciones', 'Residentes'],
      filas: datos.cargaResidentes.map((barra) => [barra.etiqueta, barra.valor]),
    },
    {
      nombre: 'Rotaciones por servicio',
      columnas: ['Servicio', 'Rotaciones'],
      filas: [...datos.rotacionesPorServicio]
        .sort((a, b) => b.valor - a.valor || a.etiqueta.localeCompare(b.etiqueta))
        .map((servicio) => [servicio.etiqueta, servicio.valor]),
    },
    {
      nombre: 'Rotaciones por mes',
      columnas: ['Mes de inicio', 'Rotaciones'],
      filas: datos.tendenciaInicios.map((punto) => [punto.etiqueta, punto.valor]),
    },
  ];
  // Con una sola universidad el reparto es siempre 100%, igual que en pantalla se omite.
  if (datos.distribucionUniversidad.length > 1) {
    hojas.push({
      nombre: 'Distribución por universidad',
      columnas: ['Universidad', 'Porcentaje de residentes'],
      filas: datos.distribucionUniversidad.map((parte) => [parte.etiqueta, parte.porcentaje]),
    });
  }
  return hojas;
}

/** Las cifras enteras se escriben como número para que Excel pueda sumarlas. */
function numeroSiEs(valor: string): string | number {
  return /^\d+$/.test(valor) ? Number(valor) : valor;
}

function fechaHora(fecha: Date) {
  const dos = (numero: number) => String(numero).padStart(2, '0');
  return (
    `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())} ` +
    `${dos(fecha.getHours())}:${dos(fecha.getMinutes())}`
  );
}
