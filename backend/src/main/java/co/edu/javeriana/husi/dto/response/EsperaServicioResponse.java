package co.edu.javeriana.husi.dto.response;

import java.time.LocalDate;

/** Residente en espera en otra rotación del mismo servicio, en orden de llegada. */
public record EsperaServicioResponse(
  Integer residenteId,
  String nombre,
  String documento,
  Integer asignaturaId,
  String asignatura,
  LocalDate inicio,
  LocalDate fin,
  /** Ya tiene una rotación asignada que se cruza con las fechas de destino. */
  boolean cruce
) {}
