package co.edu.javeriana.husi.dto.response;

import java.time.LocalDate;

/** Residente asignado hoy a una rotación del servicio que coordina el usuario en sesión. */
public record ResidenteACargoResponse(
  Integer residenteId,
  String nombre,
  String documento,
  String universidad,
  String programa,
  String asignatura,
  LocalDate inicio,
  LocalDate fin
) {}
