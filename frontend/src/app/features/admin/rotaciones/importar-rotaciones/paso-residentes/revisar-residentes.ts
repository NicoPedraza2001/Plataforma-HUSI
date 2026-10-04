import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { OpcionCatalogo } from '../../../../../models/opcion-catalogo.model';
import { Programa } from '../../../../../models/programa.model';
import { ResidenteDetalle, ResidenteRequest } from '../../../../../models/residente.model';
import { TipoDocumento } from '../../../../../models/tipo-documento.model';
import {
  agruparFilas,
  claveDeTexto,
  esMismoTexto,
  EstadoEmparejamiento,
  valoresDistintos,
} from '../emparejamiento';

/** Catálogos de la base de datos con los que se revisa cada residente. */
export interface CatalogosResidente {
  tiposDocumento: TipoDocumento[];
  perfilesAcademicos: OpcionCatalogo[];
  programas: Programa[];
}

/** Datos del residente tal como vienen en sus filas del Excel. */
export interface DatosResidenteEnExcel {
  nombre: string;
  tipoDocumento: string;
  documento: string;
  perfilAcademico: string;
  programa: string;
  universidad: string;
}

/** Un residente del Excel (todas sus filas) comparado con el registro de la base de datos. */
export interface RevisionResidente {
  clave: string;
  numerosFila: number[];
  datosEnExcel: DatosResidenteEnExcel;
  /** Código del catálogo que corresponde al tipo del Excel (acepta código o nombre). */
  codigoTipoDocumento: string | null;
  /** Nombre del catálogo para el perfil del Excel; '' si el Excel no trae perfil. */
  nombrePerfilAcademico: string | null;
  programa: Programa | null;
  residenteRegistrado: ResidenteDetalle | null;
  estado: EstadoEmparejamiento;
  observaciones: string[];
}

export function buscarCodigoTipoDocumento(
  valorEnExcel: string,
  tiposDocumento: readonly TipoDocumento[],
): string | null {
  const tipo = tiposDocumento.find(
    (tipoDocumento) =>
      esMismoTexto(tipoDocumento.codigo, valorEnExcel) ||
      esMismoTexto(tipoDocumento.nombre, valorEnExcel),
  );
  return tipo?.codigo ?? null;
}

export function buscarNombrePerfilAcademico(
  valorEnExcel: string,
  perfilesAcademicos: readonly OpcionCatalogo[],
): string | null {
  if (!valorEnExcel.trim()) return '';
  return (
    perfilesAcademicos.find((perfil) => esMismoTexto(perfil.nombre, valorEnExcel))?.nombre ?? null
  );
}

export function buscarProgramaPorNombres(
  programas: readonly Programa[],
  nombreUniversidad: string,
  nombrePrograma: string,
): Programa | null {
  return (
    programas.find(
      (programa) =>
        esMismoTexto(programa.universidad, nombreUniversidad) &&
        esMismoTexto(programa.nombre, nombrePrograma),
    ) ?? null
  );
}

export function describirPrograma(programa: Programa | null | undefined): string {
  return programa ? `${programa.nombre} — ${programa.universidad}` : 'sin programa';
}

/** Clave del residente: su documento (el backend lo busca sin distinguir mayúsculas). */
export function claveDeDocumento(documento: string): string {
  return claveDeTexto(documento);
}

function leerDatosEnExcel(fila: FilaCargaInicial): DatosResidenteEnExcel {
  return {
    nombre: fila.residente.trim(),
    tipoDocumento: fila.tipoDocumento.trim(),
    documento: fila.documento.trim(),
    perfilAcademico: fila.perfilAcademico.trim(),
    programa: fila.programa.trim(),
    universidad: fila.universidad.trim(),
  };
}

/** Avisa si las filas de un mismo documento no traen los mismos datos del residente. */
function buscarFilasInconsistentes(filasDelResidente: readonly FilaCargaInicial[]): string[] {
  const camposAComparar = [
    { campo: 'residente', etiqueta: 'nombre' },
    { campo: 'tipoDocumento', etiqueta: 'tipo de documento' },
    { campo: 'perfilAcademico', etiqueta: 'perfil académico' },
    { campo: 'programa', etiqueta: 'programa' },
  ] as const;
  return camposAComparar
    .map(({ campo, etiqueta }) => ({
      etiqueta,
      valores: valoresDistintos(filasDelResidente, campo),
    }))
    .filter(({ valores }) => valores.length > 1)
    .map(
      ({ etiqueta, valores }) => `Sus filas traen distinto ${etiqueta}: ${valores.join(' / ')}.`,
    );
}

/**
 * Agrupa las filas por documento y compara cada residente con la base de datos, con las
 * mismas reglas que la validación final del backend: el residente existente debe estar
 * activo y coincidir en nombre, tipo de documento, perfil (si el Excel lo trae) y programa.
 */
export function revisarResidentes(
  filas: readonly FilaCargaInicial[],
  catalogos: CatalogosResidente,
  residentesPorDocumento: ReadonlyMap<string, ResidenteDetalle>,
): RevisionResidente[] {
  // Las filas sin documento no se pueden agrupar: cada una se revisa por separado.
  const filasPorResidente = agruparFilas(filas, (fila) =>
    fila.documento.trim() ? claveDeDocumento(fila.documento) : `sin-documento-fila-${fila.fila}`,
  );

  return [...filasPorResidente].map(([clave, filasDelResidente]) => {
    const datosEnExcel = leerDatosEnExcel(filasDelResidente[0]);
    const codigoTipoDocumento = buscarCodigoTipoDocumento(
      datosEnExcel.tipoDocumento,
      catalogos.tiposDocumento,
    );
    const nombrePerfilAcademico = buscarNombrePerfilAcademico(
      datosEnExcel.perfilAcademico,
      catalogos.perfilesAcademicos,
    );
    const programa = buscarProgramaPorNombres(
      catalogos.programas,
      datosEnExcel.universidad,
      datosEnExcel.programa,
    );
    const residenteRegistrado = datosEnExcel.documento
      ? (residentesPorDocumento.get(claveDeDocumento(datosEnExcel.documento)) ?? null)
      : null;

    const observaciones = buscarFilasInconsistentes(filasDelResidente);
    if (codigoTipoDocumento === null) {
      observaciones.push(
        datosEnExcel.tipoDocumento
          ? `Tipo de documento «${datosEnExcel.tipoDocumento}» no está en el catálogo.`
          : 'Falta el tipo de documento.',
      );
    }
    if (nombrePerfilAcademico === null) {
      observaciones.push(
        `Perfil académico «${datosEnExcel.perfilAcademico}» no está en el catálogo.`,
      );
    }
    if (!programa) {
      observaciones.push(
        `El programa «${datosEnExcel.programa}» de «${datosEnExcel.universidad}» no está registrado.`,
      );
    }

    const revision: RevisionResidente = {
      clave,
      numerosFila: filasDelResidente.map((fila) => fila.fila),
      datosEnExcel,
      codigoTipoDocumento,
      nombrePerfilAcademico,
      programa,
      residenteRegistrado,
      estado: 'encontrado',
      observaciones,
    };

    if (!residenteRegistrado) {
      if (!datosEnExcel.documento) observaciones.unshift('Falta el número de documento.');
      if (!datosEnExcel.nombre) observaciones.unshift('Falta el nombre del residente.');
      return { ...revision, estado: observaciones.length ? 'incompleto' : 'nuevo' };
    }

    observaciones.push(...compararConRegistro(revision, residenteRegistrado, catalogos.programas));
    if (residenteRegistrado.estado !== 'Activo') {
      observaciones.unshift('Existe pero está inactivo: actívalo para asignarle rotaciones.');
      return { ...revision, estado: 'inactivo' };
    }
    return { ...revision, estado: observaciones.length ? 'diferencias' : 'encontrado' };
  });
}

/** Diferencias entre el Excel y el residente registrado que harían fallar la importación. */
function compararConRegistro(
  revision: RevisionResidente,
  residenteRegistrado: ResidenteDetalle,
  programas: readonly Programa[],
): string[] {
  const diferencias: string[] = [];
  const { datosEnExcel } = revision;
  if (datosEnExcel.nombre && !esMismoTexto(datosEnExcel.nombre, residenteRegistrado.nombre)) {
    diferencias.push(
      `Nombre: Excel «${datosEnExcel.nombre}»; sistema «${residenteRegistrado.nombre}».`,
    );
  }
  if (
    revision.codigoTipoDocumento &&
    !esMismoTexto(revision.codigoTipoDocumento, residenteRegistrado.tipoDocumento)
  ) {
    diferencias.push(
      `Tipo de documento: Excel ${revision.codigoTipoDocumento}; sistema ${residenteRegistrado.tipoDocumento}.`,
    );
  }
  const perfilRegistrado = residenteRegistrado.perfilAcademico ?? '';
  if (
    revision.nombrePerfilAcademico &&
    !esMismoTexto(revision.nombrePerfilAcademico, perfilRegistrado)
  ) {
    diferencias.push(
      perfilRegistrado
        ? `Perfil: Excel ${revision.nombrePerfilAcademico}; sistema ${perfilRegistrado}.`
        : `Perfil: el sistema no lo tiene; el Excel indica ${revision.nombrePerfilAcademico}.`,
    );
  }
  if (revision.programa && revision.programa.id !== residenteRegistrado.programaId) {
    const programaRegistrado = programas.find(
      (programa) => programa.id === residenteRegistrado.programaId,
    );
    diferencias.push(
      `Programa: Excel «${describirPrograma(revision.programa)}»; sistema «${describirPrograma(programaRegistrado)}».`,
    );
  }
  return diferencias;
}

/**
 * Datos iniciales del formulario: los del Excel cuando son válidos y, si faltan, los del
 * residente registrado. Así el administrador solo completa lo que falta.
 */
export function prepararDatosFormulario(revision: RevisionResidente): ResidenteRequest {
  const { datosEnExcel, residenteRegistrado } = revision;
  return {
    nombre: datosEnExcel.nombre || residenteRegistrado?.nombre || '',
    tipoDocumento: revision.codigoTipoDocumento ?? residenteRegistrado?.tipoDocumento ?? '',
    numeroDocumento: datosEnExcel.documento || residenteRegistrado?.numeroDocumento || '',
    perfilAcademico: revision.nombrePerfilAcademico || residenteRegistrado?.perfilAcademico || '',
    programaId: revision.programa?.id ?? residenteRegistrado?.programaId ?? null,
  };
}
