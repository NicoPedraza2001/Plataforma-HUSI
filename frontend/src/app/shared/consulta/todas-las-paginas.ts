import { firstValueFrom, Observable } from 'rxjs';
import { Pagina } from '../../models/pagina.model';

/** Máximo que aceptan los listados paginados del backend (`tamano` ≤ 100). */
export const TAMANO_PAGINA_MAXIMO = 100;

/**
 * Recorre un listado paginado hasta la última página y devuelve todo su contenido.
 * Úsalo solo cuando la pantalla necesite el catálogo completo (p. ej. para emparejar
 * datos de un Excel); los listados visibles deben seguir paginando en el backend.
 */
export async function consultarTodasLasPaginas<T>(
  consultarPagina: (numeroPagina: number, tamanoPagina: number) => Observable<Pagina<T>>,
): Promise<T[]> {
  const registros: T[] = [];
  let numeroPagina = 1;
  let totalPaginas = 1;
  do {
    const pagina = await firstValueFrom(consultarPagina(numeroPagina, TAMANO_PAGINA_MAXIMO));
    registros.push(...pagina.contenido);
    totalPaginas = pagina.totalPaginas;
    numeroPagina++;
  } while (numeroPagina <= totalPaginas);
  return registros;
}
