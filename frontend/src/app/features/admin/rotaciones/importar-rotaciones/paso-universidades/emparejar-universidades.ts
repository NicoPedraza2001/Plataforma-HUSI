import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { UniversidadDetalle } from '../../../../../models/universidad.model';
import {
  agruparFilas,
  buscarSugerencias,
  buscarValoresOriginales,
  claveDeTexto,
  esMismoTexto,
  GrupoEmparejamiento,
  RegistroDelSistema,
  valoresDistintos,
} from '../emparejamiento';

function convertirEnRegistro(universidad: UniversidadDetalle): RegistroDelSistema {
  return { id: universidad.id, nombre: universidad.nombre, estado: universidad.estado };
}

export function buscarUniversidadPorNombre(
  universidades: readonly UniversidadDetalle[],
  nombre: string,
): UniversidadDetalle | null {
  return universidades.find((universidad) => esMismoTexto(universidad.nombre, nombre)) ?? null;
}

/**
 * Agrupa las filas por universidad y compara cada una con las registradas, con las mismas
 * reglas del backend: nombre sin distinguir mayúsculas, debe estar activa y, si el Excel
 * trae país, debe coincidir con el registrado.
 */
export function emparejarUniversidades(
  filas: readonly FilaCargaInicial[],
  filasOriginalesPorNumero: ReadonlyMap<number, FilaCargaInicial>,
  universidadesDelSistema: readonly UniversidadDetalle[],
): GrupoEmparejamiento[] {
  const universidadesActivas = universidadesDelSistema.filter(
    (universidad) => universidad.estado === 'Activo',
  );
  const opcionesParaAsociar = universidadesActivas.map(convertirEnRegistro);
  const filasPorUniversidad = agruparFilas(filas, (fila) => claveDeTexto(fila.universidad));

  return [...filasPorUniversidad].map(([clave, filasDelGrupo]) => {
    const nombreEnExcel = filasDelGrupo[0].universidad.trim();
    const numerosFila = filasDelGrupo.map((fila) => fila.fila);
    const paisesEnExcel = valoresDistintos(filasDelGrupo, 'pais');
    const universidadRegistrada = nombreEnExcel
      ? buscarUniversidadPorNombre(universidadesDelSistema, nombreEnExcel)
      : null;
    const grupo: GrupoEmparejamiento = {
      clave,
      valorEnExcel: nombreEnExcel,
      valoresOriginales: buscarValoresOriginales(
        filasOriginalesPorNumero,
        numerosFila,
        'universidad',
        nombreEnExcel,
      ),
      contexto: paisesEnExcel.join(', '),
      contextoId: null,
      numerosFila,
      estado: 'encontrado',
      registro: universidadRegistrada ? convertirEnRegistro(universidadRegistrada) : null,
      observaciones: [],
      sugerencias: [],
      opciones: opcionesParaAsociar,
      sePuedeCrear: true,
    };

    if (!nombreEnExcel) {
      return {
        ...grupo,
        estado: 'incompleto',
        observaciones: ['Estas filas no tienen universidad.'],
      };
    }
    if (!universidadRegistrada) {
      return {
        ...grupo,
        estado: 'nuevo',
        observaciones: paisesEnExcel.length ? [] : ['Indica el país al crearla.'],
        sugerencias: buscarSugerencias(nombreEnExcel, universidadesActivas).map(
          convertirEnRegistro,
        ),
      };
    }
    if (universidadRegistrada.estado !== 'Activo') {
      return {
        ...grupo,
        estado: 'inactivo',
        observaciones: ['Existe pero está inactiva: actívala o elige otra.'],
      };
    }
    const paisesDistintos = paisesEnExcel.filter(
      (pais) => !esMismoTexto(pais, universidadRegistrada.pais),
    );
    if (paisesDistintos.length) {
      return {
        ...grupo,
        estado: 'diferencias',
        observaciones: [
          `País en el Excel: ${paisesDistintos.join(', ')}; en el sistema: ${universidadRegistrada.pais}.`,
        ],
      };
    }
    return grupo;
  });
}
