package co.edu.javeriana.husi.dto.response;

public record ResidenteResponse(
  Integer id,
  String nombre,
  String tipoDocumento,
  String numeroDocumento,
  String perfilAcademico,
  Integer programaId,
  String estado
) {}
