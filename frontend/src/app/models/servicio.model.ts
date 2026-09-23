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

/** Cuerpo para crear o editar; el estado se cambia por separado. */
export interface ServicioRequest {
  nombre: string;
  descripcion: string | null;
  cupoSimultaneo: number | null;
  cupoMaximoInstalado: number | null;
}
