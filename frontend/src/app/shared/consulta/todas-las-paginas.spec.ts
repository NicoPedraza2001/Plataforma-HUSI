import { of } from 'rxjs';
import { consultarTodasLasPaginas } from './todas-las-paginas';

describe('consultarTodasLasPaginas', () => {
  it('junta el contenido de todas las páginas con el tamaño máximo', async () => {
    const paginasSolicitadas: number[] = [];
    const registros = await consultarTodasLasPaginas((numeroPagina, tamanoPagina) => {
      paginasSolicitadas.push(numeroPagina);
      expect(tamanoPagina).toBe(100);
      return of({
        contenido: [`registro-${numeroPagina}`],
        totalElementos: 3,
        totalPaginas: 3,
        pagina: numeroPagina,
        tamano: tamanoPagina,
      });
    });
    expect(paginasSolicitadas).toEqual([1, 2, 3]);
    expect(registros).toEqual(['registro-1', 'registro-2', 'registro-3']);
  });
});
