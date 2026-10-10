/**
 * Datos ficticios para las pruebas de los pasos de importación. No se usa en la aplicación:
 * solo lo importan los `*.spec.ts` de esta carpeta.
 */
import { FilaCargaInicial } from '../../../../models/carga-inicial.model';
import { Asignatura } from '../../../../models/asignatura.model';
import { Programa } from '../../../../models/programa.model';
import { Servicio } from '../../../../models/servicio.model';
import { UniversidadDetalle } from '../../../../models/universidad.model';

export function crearFilaDePrueba(cambios: Partial<FilaCargaInicial> = {}): FilaCargaInicial {
  return {
    fila: 2,
    universidad: 'Pontificia Universidad Javeriana',
    pais: 'Colombia',
    programa: 'Medicina Interna',
    residente: 'Ana Pérez',
    tipoDocumento: 'CC',
    documento: '001',
    perfilAcademico: '',
    servicio: 'UCI',
    cupoMaximo: '',
    cupoSimultaneo: '',
    asignatura: 'Cuidado intensivo',
    inicio: '2099-01-01',
    fin: '2099-01-31',
    ...cambios,
  };
}

export const UNIVERSIDAD_JAVERIANA: UniversidadDetalle = {
  id: 1,
  nombre: 'Pontificia Universidad Javeriana',
  pais: 'Colombia',
  estado: 'Activo',
};

export const UNIVERSIDAD_ROSARIO_INACTIVA: UniversidadDetalle = {
  id: 2,
  nombre: 'Universidad del Rosario',
  pais: 'Colombia',
  estado: 'Inactivo',
};

/** Universidad externa a la Javeriana: sus programas son de convenio. */
export const UNIVERSIDAD_ANDES: UniversidadDetalle = {
  id: 3,
  nombre: 'Universidad de los Andes',
  pais: 'Colombia',
  estado: 'Activo',
};

export const UNIVERSIDAD_BARCELONA: UniversidadDetalle = {
  id: 4,
  nombre: 'Universidad de Barcelona',
  pais: 'España',
  estado: 'Activo',
};

export const PROGRAMA_MEDICINA_INTERNA: Programa = {
  id: 10,
  nombre: 'Medicina Interna',
  universidadId: 1,
  universidad: 'Pontificia Universidad Javeriana',
  estado: 'Activo',
};

export const PROGRAMA_PEDIATRIA: Programa = {
  id: 11,
  nombre: 'Pediatría',
  universidadId: 1,
  universidad: 'Pontificia Universidad Javeriana',
  estado: 'Activo',
};

export const SERVICIO_UCI: Servicio = {
  id: 20,
  nombre: 'UCI',
  descripcion: null,
  cupoMaximoInstalado: 8,
  cupoSimultaneo: 4,
  estado: 'Activo',
  activo: true,
};

export const SERVICIO_ALTO_RIESGO_INACTIVO: Servicio = {
  id: 21,
  nombre: 'Alto Riesgo',
  descripcion: null,
  cupoMaximoInstalado: 10,
  cupoSimultaneo: 5,
  estado: 'Inactivo',
  activo: false,
};

export const ASIGNATURA_CUIDADO_INTENSIVO: Asignatura = {
  id: 40,
  nombre: 'Cuidado intensivo',
  servicioId: 20,
  servicio: 'UCI',
  estado: 'Activo',
  activo: true,
};

export function crearPaginaDePrueba<T>(contenido: T[]) {
  return { contenido, totalElementos: contenido.length, totalPaginas: 1, pagina: 1, tamano: 100 };
}

/** Deja terminar las cargas asíncronas de un paso (promesas encadenadas sobre `of()`). */
export function esperarCargas(): Promise<void> {
  return new Promise((resolver) => setTimeout(resolver, 0));
}
