package co.edu.javeriana.husi.dto.response;

public record ServicioResponse(
  Integer id,
  String nombre,
  String descripcion,
  Integer cupoSimultaneo,
  Integer cupoMaximoInstalado,
  String estado,
  boolean activo
) {}
