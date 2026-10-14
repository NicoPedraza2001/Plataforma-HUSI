package co.edu.javeriana.husi.dto.response;

/** {@code servicioId} y {@code servicio} solo vienen con valor si el usuario es coordinador. */
public record UsuarioResponse(
  Integer id,
  String nombre,
  String apellido,
  String correo,
  Integer rolId,
  String estado,
  Integer servicioId,
  String servicio
) {}
