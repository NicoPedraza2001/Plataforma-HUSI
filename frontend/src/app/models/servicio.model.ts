/** Contratos de respuesta y solicitudes del dominio; conserva los campos de la API. */
export interface Servicio {
  id: number;
  nombre: string;
  /** Opcional en la base de datos: puede llegar sin diligenciar. */
  descripcion: string | null;
  cupoSimultaneo: number;
  cupoMaximoInstalado: number;
  activo: boolean;
}
