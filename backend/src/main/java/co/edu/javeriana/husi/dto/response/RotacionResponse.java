package co.edu.javeriana.husi.dto.response;

/** Contrato estable de la gestión de rotaciones (estado administrativo separado de cupos). */
public record RotacionResponse(
  Integer asignaturaId,
  String inicio,
  String fin,
  String asignatura,
  String servicio,
  Integer servicioId,
  boolean activa,
  String disponibilidad,
  int cupoDisponible,
  long relaciones,
  /** La fecha final ya pasó: la rotación es de solo lectura. */
  boolean finalizada
) {}
