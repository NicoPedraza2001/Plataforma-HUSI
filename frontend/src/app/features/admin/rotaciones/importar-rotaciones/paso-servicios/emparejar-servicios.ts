import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { Servicio } from '../../../../../models/servicio.model';
import {
  agruparFilas,
  buscarSugerencias,
  buscarValoresOriginales,
  claveDeTexto,
  esMismoTexto,
  GrupoEmparejamiento,
  leerCupo,
  RegistroDelSistema,
  valoresDistintos,
} from '../emparejamiento';

/** Cupos que trae el Excel para un servicio, solo si son enteros válidos y coherentes. */
export interface CuposEnExcel {
  cupoMaximoInstalado: number;
  cupoSimultaneo: number;
}

function convertirEnRegistro(servicio: Servicio): RegistroDelSistema {
  return { id: servicio.id, nombre: servicio.nombre, estado: servicio.estado };
}

export function buscarServicioPorNombre(
  servicios: readonly Servicio[],
  nombre: string,
): Servicio | null {
  return servicios.find((servicio) => esMismoTexto(servicio.nombre, nombre)) ?? null;
}

export function describirCupos(cupoMaximo: string, cupoSimultaneo: string): string {
  if (!cupoMaximo && !cupoSimultaneo) return 'Sin cupos en el Excel';
  return `Máximo ${cupoMaximo || '—'} · simultáneo ${cupoSimultaneo || '—'}`;
}

/**
 * Cupos únicos del grupo listos para crear el servicio: un solo valor de cada cupo,
 * enteros y con el simultáneo sin superar el máximo (las reglas del backend).
 */
export function leerCuposDelGrupo(filasDelGrupo: readonly FilaCargaInicial[]): CuposEnExcel | null {
  const cuposMaximos = valoresDistintos(filasDelGrupo, 'cupoMaximo');
  const cuposSimultaneos = valoresDistintos(filasDelGrupo, 'cupoSimultaneo');
  if (cuposMaximos.length !== 1 || cuposSimultaneos.length !== 1) return null;
  const cupoMaximoInstalado = leerCupo(cuposMaximos[0]);
  const cupoSimultaneo = leerCupo(cuposSimultaneos[0]);
  if (cupoMaximoInstalado === null || cupoSimultaneo === null) return null;
  if (cupoSimultaneo > cupoMaximoInstalado) return null;
  return { cupoMaximoInstalado, cupoSimultaneo };
}

/** Diferencias de cupos que harían fallar la importación de un servicio existente. */
function compararCupos(
  filasDelGrupo: readonly FilaCargaInicial[],
  servicioRegistrado: Servicio,
): string[] {
  const diferencias: string[] = [];
  const cupos = [
    {
      campo: 'cupoMaximo' as const,
      etiqueta: 'Cupo máximo',
      valorRegistrado: servicioRegistrado.cupoMaximoInstalado,
    },
    {
      campo: 'cupoSimultaneo' as const,
      etiqueta: 'Cupo simultáneo',
      valorRegistrado: servicioRegistrado.cupoSimultaneo,
    },
  ];
  for (const { campo, etiqueta, valorRegistrado } of cupos) {
    // El backend solo compara los cupos que trae el Excel; los vacíos no se revisan.
    const valoresDistintosDelRegistro = valoresDistintos(filasDelGrupo, campo).filter(
      (valorEnExcel) => leerCupo(valorEnExcel) !== valorRegistrado,
    );
    if (valoresDistintosDelRegistro.length) {
      diferencias.push(
        `${etiqueta} en el Excel: ${valoresDistintosDelRegistro.join(', ')}; en el sistema: ${valorRegistrado}.`,
      );
    }
  }
  return diferencias;
}

/**
 * Agrupa las filas por servicio y compara cada uno con los registrados, con las reglas
 * del backend: nombre sin distinguir mayúsculas, debe estar activo y, si el Excel trae
 * cupos, deben coincidir con los registrados. Un servicio nuevo necesita sus dos cupos.
 */
export function emparejarServicios(
  filas: readonly FilaCargaInicial[],
  filasOriginalesPorNumero: ReadonlyMap<number, FilaCargaInicial>,
  serviciosDelSistema: readonly Servicio[],
): GrupoEmparejamiento[] {
  const serviciosActivos = serviciosDelSistema.filter((servicio) => servicio.activo);
  const opcionesParaAsociar = serviciosActivos.map(convertirEnRegistro);
  const filasPorServicio = agruparFilas(filas, (fila) => claveDeTexto(fila.servicio));

  return [...filasPorServicio].map(([clave, filasDelGrupo]) => {
    const nombreEnExcel = filasDelGrupo[0].servicio.trim();
    const numerosFila = filasDelGrupo.map((fila) => fila.fila);
    const servicioRegistrado = nombreEnExcel
      ? buscarServicioPorNombre(serviciosDelSistema, nombreEnExcel)
      : null;
    const grupo: GrupoEmparejamiento = {
      clave,
      valorEnExcel: nombreEnExcel,
      valoresOriginales: buscarValoresOriginales(
        filasOriginalesPorNumero,
        numerosFila,
        'servicio',
        nombreEnExcel,
      ),
      contexto: describirCupos(
        valoresDistintos(filasDelGrupo, 'cupoMaximo').join(', '),
        valoresDistintos(filasDelGrupo, 'cupoSimultaneo').join(', '),
      ),
      contextoId: null,
      numerosFila,
      estado: 'encontrado',
      registro: servicioRegistrado ? convertirEnRegistro(servicioRegistrado) : null,
      observaciones: [],
      sugerencias: [],
      opciones: opcionesParaAsociar,
      sePuedeCrear: true,
    };

    if (!nombreEnExcel) {
      return { ...grupo, estado: 'incompleto', observaciones: ['Estas filas no tienen servicio.'] };
    }
    if (!servicioRegistrado) {
      return {
        ...grupo,
        estado: 'nuevo',
        observaciones: leerCuposDelGrupo(filasDelGrupo)
          ? []
          : ['Indica el cupo máximo y el simultáneo al crearlo.'],
        sugerencias: buscarSugerencias(nombreEnExcel, serviciosActivos).map(convertirEnRegistro),
      };
    }
    if (!servicioRegistrado.activo) {
      return {
        ...grupo,
        estado: 'inactivo',
        observaciones: ['Existe pero está inactivo: actívalo o elige otro.'],
      };
    }
    const diferenciasDeCupos = compararCupos(filasDelGrupo, servicioRegistrado);
    if (diferenciasDeCupos.length) {
      return { ...grupo, estado: 'diferencias', observaciones: diferenciasDeCupos };
    }
    return {
      ...grupo,
      contexto: `${grupo.contexto} (sistema: máximo ${servicioRegistrado.cupoMaximoInstalado} · simultáneo ${servicioRegistrado.cupoSimultaneo})`,
    };
  });
}
