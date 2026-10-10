/** Backend: dto/response/UniversidadCatalogoResponse (solo universidades activas). */
export interface UniversidadCatalogo {
  id: number;
  nombre: string;
  pais: string;
}
/** Backend: dto/response/UniversidadResponse. */
export interface UniversidadDetalle extends UniversidadCatalogo {
  estado: 'Activo' | 'Inactivo';
}
/** Backend: dto/request/UniversidadRequest. */
export interface UniversidadDatos {
  nombre: string;
  pais: string;
}

export type Universidad = UniversidadCatalogo;
