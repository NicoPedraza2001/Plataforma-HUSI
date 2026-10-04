package co.edu.javeriana.husi.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Fecha del hospital (Bogotá) y su formato colombiano para los mensajes. */
public final class Calendario {

  private static final ZoneId ZONA = ZoneId.of("America/Bogota");
  private static final DateTimeFormatter FORMATO =
    DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private Calendario() {}

  public static LocalDate hoy() {
    return LocalDate.now(ZONA);
  }

  public static String formato(LocalDate fecha) {
    return fecha.format(FORMATO);
  }

  /** Una rotación termina cuando su fecha final ya pasó. */
  public static boolean finalizada(LocalDate fin) {
    return fin.isBefore(hoy());
  }
}
