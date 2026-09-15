package co.edu.javeriana.husi.dto.response;

public record UniversidadResponse(
  Integer id,
  String nombre,
  String pais,
  String perfil,
  String estado
) {}
