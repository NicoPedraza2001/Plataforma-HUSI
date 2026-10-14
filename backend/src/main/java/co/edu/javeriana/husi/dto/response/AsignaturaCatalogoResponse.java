package co.edu.javeriana.husi.dto.response;

/** Opción de asignatura para filtros: el nombre se repite entre servicios, por eso va el servicio. */
public record AsignaturaCatalogoResponse(
  Integer id,
  String nombre,
  String servicio
) {}
