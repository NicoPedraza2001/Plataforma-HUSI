package co.edu.javeriana.husi.dto.response;

/** Programa, servicio y asignatura que aparecen juntos en alguna rotación. */
public record RelacionFiltrosResponse(
  String programa,
  String servicio,
  Integer asignaturaId
) {}
