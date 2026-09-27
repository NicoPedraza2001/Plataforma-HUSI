package co.edu.javeriana.husi.dto.response;

public record CoordinadorResponse(
  Integer id,
  Integer usuarioId,
  String nombre,
  String correo,
  Integer servicioId,
  String servicio,
  String estado,
  boolean activo
) {}
