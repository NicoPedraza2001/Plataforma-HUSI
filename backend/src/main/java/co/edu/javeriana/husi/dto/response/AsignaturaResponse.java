package co.edu.javeriana.husi.dto.response;

public record AsignaturaResponse(
  Integer id,
  String nombre,
  Integer servicioId,
  String servicio,
  String estado,
  boolean activo
) {}
