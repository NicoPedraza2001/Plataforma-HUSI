import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { Programa } from '../../../../../models/programa.model';
import { UniversidadDetalle } from '../../../../../models/universidad.model';
import {
  agruparFilas,
  buscarSugerencias,
  buscarValoresOriginales,
  claveDeTexto,
  esMismoTexto,
  GrupoEmparejamiento,
  RegistroDelSistema,
} from '../emparejamiento';
import { buscarUniversidadPorNombre } from '../paso-universidades/emparejar-universidades';
import {
  buscarConvenio,
  buscarErrorDeConvenioSegunPais,
  convenioSegunPais,
  esUniversidadPropia,
  etiquetaConvenio,
} from '../../../programas/programas-de-convenio';

function convertirEnRegistro(programa: Programa): RegistroDelSistema {
  return { id: programa.id, nombre: programa.nombre, estado: programa.estado };
}

/** Programa registrado de una universidad con ese nombre (sin distinguir mayúsculas). */
export function buscarProgramaDeUniversidad(
  programas: readonly Programa[],
  universidadId: number,
  nombrePrograma: string,
): Programa | null {
  return (
    programas.find(
      (programa) =>
        programa.universidadId === universidadId && esMismoTexto(programa.nombre, nombrePrograma),
    ) ?? null
  );
}

/**
 * Agrupa las filas por la pareja universidad + programa y compara cada programa con los
 * registrados en esa universidad. La universidad ya debe estar resuelta en el paso anterior.
 */
export function emparejarProgramas(
  filas: readonly FilaCargaInicial[],
  filasOriginalesPorNumero: ReadonlyMap<number, FilaCargaInicial>,
  universidadesDelSistema: readonly UniversidadDetalle[],
  programasDelSistema: readonly Programa[],
): GrupoEmparejamiento[] {
  const filasPorPrograma = agruparFilas(
    filas,
    (fila) => `${claveDeTexto(fila.universidad)}|${claveDeTexto(fila.programa)}`,
  );

  return [...filasPorPrograma].map(([clave, filasDelGrupo]) => {
    const nombreEnExcel = filasDelGrupo[0].programa.trim();
    const nombreUniversidad = filasDelGrupo[0].universidad.trim();
    const numerosFila = filasDelGrupo.map((fila) => fila.fila);
    const universidad = buscarUniversidadPorNombre(universidadesDelSistema, nombreUniversidad);
    const programasDeLaUniversidad = universidad
      ? programasDelSistema.filter((programa) => programa.universidadId === universidad.id)
      : [];
    // Una universidad nacional nunca puede elegir «Convenio Internacional».
    const programasActivos = programasDeLaUniversidad.filter(
      (programa) =>
        programa.estado === 'Activo' &&
        !(universidad && buscarErrorDeConvenioSegunPais(programa.nombre, universidad)),
    );
    const programaRegistrado =
      universidad && nombreEnExcel
        ? buscarProgramaDeUniversidad(programasDeLaUniversidad, universidad.id, nombreEnExcel)
        : null;
    const grupo: GrupoEmparejamiento = {
      clave,
      valorEnExcel: nombreEnExcel,
      valoresOriginales: buscarValoresOriginales(
        filasOriginalesPorNumero,
        numerosFila,
        'programa',
        nombreEnExcel,
      ),
      contexto: universidad?.nombre ?? nombreUniversidad,
      contextoId: universidad?.id ?? null,
      numerosFila,
      estado: 'encontrado',
      registro: programaRegistrado ? convertirEnRegistro(programaRegistrado) : null,
      observaciones: [],
      sugerencias: [],
      opciones: programasActivos.map(convertirEnRegistro),
      sePuedeCrear: !!universidad,
    };

    if (!universidad) {
      return {
        ...grupo,
        estado: 'incompleto',
        observaciones: [
          `La universidad «${nombreUniversidad || 'sin nombre'}» no está registrada: resuélvela en el paso Universidades.`,
        ],
      };
    }
    if (!nombreEnExcel) {
      return {
        ...grupo,
        estado: 'incompleto',
        observaciones: ['Estas filas no tienen programa.'],
      };
    }
    const errorDeConvenio = esUniversidadPropia(universidad.nombre)
      ? null
      : buscarErrorDeConvenioSegunPais(nombreEnExcel, universidad);
    if (errorDeConvenio) {
      return {
        ...grupo,
        estado: 'incompleto',
        registro: null,
        observaciones: [errorDeConvenio],
      };
    }
    if (!programaRegistrado && !esUniversidadPropia(universidad.nombre)) {
      const convenioSugerido = buscarConvenio(nombreEnExcel) ?? convenioSegunPais(universidad.pais);
      return {
        ...grupo,
        estado: 'nuevo',
        observaciones: [
          `«${universidad.nombre}» es externa a la Javeriana: sus filas se registran como «${etiquetaConvenio(convenioSugerido)}».`,
        ],
        sugerencias: programasActivos
          .filter((programa) => buscarConvenio(programa.nombre) !== null)
          .map(convertirEnRegistro),
      };
    }
    if (!programaRegistrado) {
      return {
        ...grupo,
        estado: 'nuevo',
        sugerencias: buscarSugerencias(nombreEnExcel, programasActivos).map(convertirEnRegistro),
      };
    }
    if (programaRegistrado.estado !== 'Activo') {
      return {
        ...grupo,
        estado: 'inactivo',
        observaciones: ['Existe pero está inactivo: actívalo o elige otro.'],
      };
    }
    return grupo;
  });
}
