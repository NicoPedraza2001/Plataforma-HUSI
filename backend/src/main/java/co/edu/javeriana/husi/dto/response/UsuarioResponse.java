package co.edu.javeriana.husi.dto.response;

public record UsuarioResponse(
  Integer id,
  String nombre,
  String apellido,
  String correo,
  Integer rolId,
  String estado
) {}
