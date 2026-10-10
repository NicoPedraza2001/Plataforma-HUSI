import { DatosRegistro } from '../../../models/historial.model';

/** Una fila de la tabla «antes / después» del detalle de un movimiento. */
export interface CampoComparado {
  campo: string;
  valorAnterior: string;
  valorNuevo: string;
  fueModificado: boolean;
}

const SIN_VALOR = '—';

/**
 * Une los datos anteriores y nuevos de un registro campo por campo, en el orden en que los
 * guardó el backend. Al crear no hay datos anteriores y al eliminar no hay datos nuevos: en
 * esos casos ningún campo se marca como modificado.
 */
export function compararDatos(
  datosAnteriores: DatosRegistro | undefined,
  datosNuevos: DatosRegistro | undefined,
): CampoComparado[] {
  const anteriores = datosAnteriores ?? {};
  const nuevos = datosNuevos ?? {};
  const hayAmbos = !!datosAnteriores && !!datosNuevos;
  const campos = [...new Set([...Object.keys(nuevos), ...Object.keys(anteriores)])];
  return campos.map((campo) => {
    const valorAnterior = mostrarValor(anteriores[campo]);
    const valorNuevo = mostrarValor(nuevos[campo]);
    return {
      campo,
      valorAnterior,
      valorNuevo,
      fueModificado: hayAmbos && valorAnterior !== valorNuevo,
    };
  });
}

function mostrarValor(valor: DatosRegistro[string] | undefined): string {
  if (valor === null || valor === undefined || valor === '') return SIN_VALOR;
  if (typeof valor === 'boolean') return valor ? 'Sí' : 'No';
  return String(valor);
}

/** Clase del distintivo según la acción: verde lo que suma, rojo lo que quita, azul lo demás. */
export function claseBadgeAccion(accion: string): string {
  if (['CREÓ', 'ACTIVÓ', 'ASIGNÓ'].includes(accion)) return 'badge badge--activo';
  if (['INACTIVÓ', 'ELIMINÓ'].includes(accion)) return 'badge badge--inactivo';
  return 'badge badge--info';
}
