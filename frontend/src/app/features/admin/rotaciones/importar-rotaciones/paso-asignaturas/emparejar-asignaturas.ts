import { Asignatura } from '../../../../../models/asignatura.model';
import { FilaCargaInicial } from '../../../../../models/carga-inicial.model';
import { Servicio } from '../../../../../models/servicio.model';
import {
  agruparFilas,
  buscarSugerencias,
  buscarValoresOriginales,
  claveDeTexto,
  esMismoTexto,
  GrupoEmparejamiento,
  RegistroDelSistema,
} from '../emparejamiento';
import { buscarServicioPorNombre } from '../paso-servicios/emparejar-servicios';

function convertirEnRegistro(asignatura: Asignatura): RegistroDelSistema {
  return { id: asignatura.id, nombre: asignatura.nombre, estado: asignatura.estado };
}

/** Asignatura registrada de un servicio con ese nombre (sin distinguir mayúsculas). */
export function buscarAsignaturaDeServicio(
  asignaturas: readonly Asignatura[],
  servicioId: number,
  nombreAsignatura: string,
): Asignatura | null {
  return (
    asignaturas.find(
      (asignatura) =>
        asignatura.servicioId === servicioId && esMismoTexto(asignatura.nombre, nombreAsignatura),
    ) ?? null
  );
}

/**
 * Agrupa las filas por la pareja servicio + asignatura y compara cada asignatura con las
 * registradas en ese servicio. El servicio ya debe estar resuelto en el paso anterior.
 */
export function emparejarAsignaturas(
  filas: readonly FilaCargaInicial[],
  filasOriginalesPorNumero: ReadonlyMap<number, FilaCargaInicial>,
  serviciosDelSistema: readonly Servicio[],
  asignaturasDelSistema: readonly Asignatura[],
): GrupoEmparejamiento[] {
  const filasPorAsignatura = agruparFilas(
    filas,
    (fila) => `${claveDeTexto(fila.servicio)}|${claveDeTexto(fila.asignatura)}`,
  );

  return [...filasPorAsignatura].map(([clave, filasDelGrupo]) => {
    const nombreEnExcel = filasDelGrupo[0].asignatura.trim();
    const nombreServicio = filasDelGrupo[0].servicio.trim();
    const numerosFila = filasDelGrupo.map((fila) => fila.fila);
    const servicio = buscarServicioPorNombre(serviciosDelSistema, nombreServicio);
    const asignaturasDelServicio = servicio
      ? asignaturasDelSistema.filter((asignatura) => asignatura.servicioId === servicio.id)
      : [];
    const asignaturasActivas = asignaturasDelServicio.filter((asignatura) => asignatura.activo);
    const asignaturaRegistrada =
      servicio && nombreEnExcel
        ? buscarAsignaturaDeServicio(asignaturasDelServicio, servicio.id, nombreEnExcel)
        : null;
    const grupo: GrupoEmparejamiento = {
      clave,
      valorEnExcel: nombreEnExcel,
      valoresOriginales: buscarValoresOriginales(
        filasOriginalesPorNumero,
        numerosFila,
        'asignatura',
        nombreEnExcel,
      ),
      contexto: servicio?.nombre ?? nombreServicio,
      contextoId: servicio?.id ?? null,
      numerosFila,
      estado: 'encontrado',
      registro: asignaturaRegistrada ? convertirEnRegistro(asignaturaRegistrada) : null,
      observaciones: [],
      sugerencias: [],
      opciones: asignaturasActivas.map(convertirEnRegistro),
      sePuedeCrear: !!servicio,
    };

    if (!servicio) {
      return {
        ...grupo,
        estado: 'incompleto',
        observaciones: [
          `El servicio «${nombreServicio || 'sin nombre'}» no está registrado: resuélvelo en el paso Servicios.`,
        ],
      };
    }
    if (!nombreEnExcel) {
      return {
        ...grupo,
        estado: 'incompleto',
        observaciones: ['Estas filas no tienen asignatura.'],
      };
    }
    if (!asignaturaRegistrada) {
      return {
        ...grupo,
        estado: 'nuevo',
        sugerencias: buscarSugerencias(nombreEnExcel, asignaturasActivas).map(convertirEnRegistro),
      };
    }
    if (!asignaturaRegistrada.activo) {
      return {
        ...grupo,
        estado: 'inactivo',
        observaciones: ['Existe pero está inactiva: actívala o elige otra.'],
      };
    }
    return grupo;
  });
}
