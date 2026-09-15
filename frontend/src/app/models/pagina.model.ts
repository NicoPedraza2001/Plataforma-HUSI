export type Parametros = Record<string, string | number | boolean>;
export interface Pagina<T> {
  contenido: T[];
  totalElementos: number;
  totalPaginas: number;
  pagina: number;
  tamano: number;
}
