export interface UniversidadCatalogo { id: number; nombre: string; }
export interface UniversidadDetalle extends UniversidadCatalogo {
  pais: string;
  perfil?: string | null;
  estado: 'Activo' | 'Inactivo';
}
export interface UniversidadDatos { nombre: string; pais: string; perfil: string | null; }


export type Universidad = UniversidadCatalogo;
