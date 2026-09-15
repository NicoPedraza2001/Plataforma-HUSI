/** Backend: `dto/response/ProgramaResponse`. */
export interface Programa {
  id: number;
  nombre: string;
  universidadId: number;
  universidad: string;
  estado: string;
}

/** Cuerpo para crear o actualizar un programa. Backend: `dto/request/ProgramaRequest`. */
export interface ProgramaRequest {
  nombre: string;
  universidadId: number;
}
