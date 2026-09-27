package co.edu.javeriana.husi.dto.response;

public record ResidenteRotacionResponse(
  Integer residenteId,
  String nombre,
  String documento,
  String programa,
  String estado
) {}
