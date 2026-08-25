/* ============================================================
   Datos de demostración del dashboard
   ------------------------------------------------------------
   TODO: todo este archivo desaparece cuando exista el backend. El
   endpoint de métricas devolverá lo mismo ya cortado por periodo,
   universidad y servicio, y el componente solo tendrá que pintarlo.

   Mientras tanto es una fuente única: los KPIs, los cuatro paneles
   y la tabla se derivan de aquí, así que las cifras no pueden
   contradecirse entre sí en pantalla.
   ============================================================ */

export const PERIODOS = ['2026-2', '2026-1', '2025-2'] as const;
export type Periodo = (typeof PERIODOS)[number];

export const UNIVERSIDADES = [
  'Javeriana',
  'Universidad de los Andes',
  'Universidad Nacional',
  'Otras',
] as const;
export type Universidad = (typeof UNIVERSIDADES)[number];

export const SERVICIOS = [
  'Urgencias',
  'Medicina interna',
  'Pediatria',
  'Cirugia general',
  'Radiologia',
] as const;
export type Servicio = (typeof SERVICIOS)[number];

/** Opción que desactiva el filtro correspondiente. */
export const TODAS = 'Todas';
export const TODOS = 'Todos';

export type FiltroUniversidad = Universidad | typeof TODAS;
export type FiltroServicio = Servicio | typeof TODOS;

export const OPCIONES_UNIVERSIDAD: readonly FiltroUniversidad[] = [TODAS, ...UNIVERSIDADES];
export const OPCIONES_SERVICIO: readonly FiltroServicio[] = [TODOS, ...SERVICIOS];

/** Cifras de un servicio en el periodo base, antes de aplicar ningún filtro. */
export interface ServicioBase {
  servicio: Servicio;
  activas: number;
  proximas: number;
  residentes: number;
  /** Capacidad del servicio. No depende de la universidad que la use. */
  cupos: number;
  /** Reparto de las rotaciones por universidad, en puntos; suma 100. */
  reparto: Record<Universidad, number>;
}

export const SERVICIOS_BASE: readonly ServicioBase[] = [
  {
    servicio: 'Urgencias',
    activas: 92,
    proximas: 14,
    residentes: 134,
    cupos: 96,
    reparto: {
      Javeriana: 45,
      'Universidad de los Andes': 22,
      'Universidad Nacional': 18,
      Otras: 15,
    },
  },
  {
    servicio: 'Medicina interna',
    activas: 86,
    proximas: 11,
    residentes: 126,
    cupos: 100,
    reparto: {
      Javeriana: 40,
      'Universidad de los Andes': 25,
      'Universidad Nacional': 20,
      Otras: 15,
    },
  },
  {
    servicio: 'Pediatria',
    activas: 68,
    proximas: 9,
    residentes: 99,
    cupos: 92,
    reparto: {
      Javeriana: 44,
      'Universidad de los Andes': 21,
      'Universidad Nacional': 16,
      Otras: 19,
    },
  },
  {
    servicio: 'Cirugia general',
    activas: 61,
    proximas: 8,
    residentes: 89,
    cupos: 70,
    reparto: {
      Javeriana: 38,
      'Universidad de los Andes': 26,
      'Universidad Nacional': 19,
      Otras: 17,
    },
  },
  {
    servicio: 'Radiologia',
    activas: 49,
    proximas: 6,
    residentes: 72,
    cupos: 98,
    reparto: {
      Javeriana: 41,
      'Universidad de los Andes': 22,
      'Universidad Nacional': 17,
      Otras: 20,
    },
  },
];

/**
 * Cuánto se mueve la demanda respecto del periodo base. La capacidad no se
 * escala: los cupos son los mismos y por eso la ocupación sube o baja sola.
 */
export const FACTOR_PERIODO: Record<Periodo, number> = {
  '2026-2': 1,
  '2026-1': 0.95,
  '2025-2': 0.88,
};

export const MESES_PERIODO: Record<Periodo, readonly string[]> = {
  '2026-2': ['Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'],
  '2026-1': ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun'],
  '2025-2': ['Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'],
};

/**
 * Forma de la curva de inicios dentro del periodo, en puntos sobre 100. El
 * total del corte se reparte con estos pesos, así que la serie siempre suma
 * exactamente las rotaciones del corte.
 */
export const FORMA_INICIOS: Record<Periodo, readonly number[]> = {
  '2026-2': [12, 18, 16, 21, 19, 14],
  '2026-1': [15, 20, 18, 17, 16, 14],
  '2025-2': [14, 17, 15, 20, 20, 14],
};

/** Estado de una rotación próxima a iniciar. */
export type EstadoRotacion = 'proxima' | 'pendiente' | 'confirmada';

export interface RotacionProxima {
  id: number;
  periodo: Periodo;
  residente: string;
  universidad: Universidad;
  servicio: Servicio;
  doctor: string;
  inicio: string;
  estado: EstadoRotacion;
}

export const ROTACIONES_PROXIMAS: readonly RotacionProxima[] = [
  {
    id: 1,
    periodo: '2026-2',
    residente: 'María José Osorio',
    universidad: 'Javeriana',
    servicio: 'Urgencias',
    doctor: 'Dra. Gabriela Pérez',
    inicio: '01/08/2026',
    estado: 'proxima',
  },
  {
    id: 2,
    periodo: '2026-2',
    residente: 'Cristian Castro',
    universidad: 'Universidad de los Andes',
    servicio: 'Radiologia',
    doctor: 'Dr. Miguel Herrera',
    inicio: '05/08/2026',
    estado: 'proxima',
  },
  {
    id: 3,
    periodo: '2026-2',
    residente: 'Violeta Bautista',
    universidad: 'Universidad Nacional',
    servicio: 'Cirugia general',
    doctor: 'Dr. Andrés Ramírez',
    inicio: '06/08/2026',
    estado: 'pendiente',
  },
  {
    id: 4,
    periodo: '2026-2',
    residente: 'Daniela Lizarazo',
    universidad: 'Otras',
    servicio: 'Pediatria',
    doctor: 'Dra. Camila Torres',
    inicio: '10/08/2026',
    estado: 'confirmada',
  },
  {
    id: 5,
    periodo: '2026-2',
    residente: 'Sebastián Peña',
    universidad: 'Javeriana',
    servicio: 'Medicina interna',
    doctor: 'Dr. Santiago Ruiz',
    inicio: '12/08/2026',
    estado: 'confirmada',
  },
  {
    id: 6,
    periodo: '2026-2',
    residente: 'Andrea Salcedo',
    universidad: 'Universidad de los Andes',
    servicio: 'Urgencias',
    doctor: 'Dra. Gabriela Pérez',
    inicio: '17/08/2026',
    estado: 'pendiente',
  },
  {
    id: 7,
    periodo: '2026-1',
    residente: 'Nicolás Vargas',
    universidad: 'Javeriana',
    servicio: 'Cirugia general',
    doctor: 'Dr. Andrés Ramírez',
    inicio: '02/02/2026',
    estado: 'confirmada',
  },
  {
    id: 8,
    periodo: '2026-1',
    residente: 'Isabela Quintero',
    universidad: 'Universidad Nacional',
    servicio: 'Pediatria',
    doctor: 'Dra. Camila Torres',
    inicio: '09/02/2026',
    estado: 'proxima',
  },
  {
    id: 9,
    periodo: '2026-1',
    residente: 'Tomás Arévalo',
    universidad: 'Otras',
    servicio: 'Medicina interna',
    doctor: 'Dr. Santiago Ruiz',
    inicio: '16/03/2026',
    estado: 'pendiente',
  },
  {
    id: 10,
    periodo: '2025-2',
    residente: 'Juliana Mesa',
    universidad: 'Javeriana',
    servicio: 'Radiologia',
    doctor: 'Dr. Miguel Herrera',
    inicio: '04/08/2025',
    estado: 'confirmada',
  },
  {
    id: 11,
    periodo: '2025-2',
    residente: 'Emilio Cárdenas',
    universidad: 'Universidad de los Andes',
    servicio: 'Urgencias',
    doctor: 'Dra. Gabriela Pérez',
    inicio: '18/09/2025',
    estado: 'confirmada',
  },
];

/** Aviso operativo del panel lateral. */
export interface Alerta {
  id: string;
  etiqueta: string;
  cantidad: number;
  detalle: string;
  nivel: 'critico' | 'alerta' | 'info';
}

/**
 * Los avisos son de la operación completa y no se cortan por periodo: una
 * rotación sin doctor hay que resolverla igual con qué filtro se esté mirando.
 */
export const ALERTAS: readonly Alerta[] = [
  {
    id: 'sin-doctor',
    etiqueta: 'Rotaciones sin doctor asignado',
    cantidad: 7,
    detalle: 'Inician en menos de 15 días',
    nivel: 'critico',
  },
  {
    id: 'convenios',
    etiqueta: 'Convenios por vencer',
    cantidad: 3,
    detalle: 'Vencen este semestre',
    nivel: 'alerta',
  },
  {
    id: 'evaluaciones',
    etiqueta: 'Evaluaciones pendientes',
    cantidad: 24,
    detalle: 'De rotaciones ya finalizadas',
    nivel: 'alerta',
  },
  {
    id: 'documentos',
    etiqueta: 'Documentos por validar',
    cantidad: 12,
    detalle: 'Residentes de ingreso reciente',
    nivel: 'info',
  },
];

/** Texto que acompaña a cada estado en la tabla de próximas rotaciones. */
export const ETIQUETAS_ESTADO: Record<EstadoRotacion, string> = {
  proxima: 'PRÓXIMA',
  pendiente: 'PENDIENTE',
  confirmada: 'CONFIRMADA',
};
