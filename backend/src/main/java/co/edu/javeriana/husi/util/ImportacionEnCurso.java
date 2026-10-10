package co.edu.javeriana.husi.util;

import java.util.Optional;

/**
 * Importación a la que pertenecen los cambios de la petición en curso.
 *
 * <p>Mientras hay una importación en curso, el historial no guarda una fila por cada registro
 * creado o modificado: los suma en una sola fila resumen identificada por {@link Lote#id()}.
 * Cada petición la atiende un único hilo, por eso basta un {@link ThreadLocal}; quien la
 * inicia debe terminarla en un {@code finally}.
 */
public final class ImportacionEnCurso {

  /**
   * @param id identificador del lote (UUID); el asistente de Excel lo reutiliza en todos sus pasos
   * @param descripcion qué se importó, p. ej. «Carga inicial desde Excel»
   * @param archivo nombre del archivo importado, si se conoce
   */
  public record Lote(String id, String descripcion, String archivo) {}

  public static final String CARGA_INICIAL = "Carga inicial desde Excel";
  public static final String IMPORTACION_PROGRAMAS = "Importación de programas desde Excel";
  public static final String IMPORTACION_ROTACIONES = "Importación de rotaciones desde Excel";

  private static final ThreadLocal<Lote> LOTE_ACTUAL = new ThreadLocal<>();

  private ImportacionEnCurso() {}

  public static Optional<Lote> consultar() {
    return Optional.ofNullable(LOTE_ACTUAL.get());
  }

  public static void iniciar(Lote lote) {
    LOTE_ACTUAL.set(lote);
  }

  public static void terminar() {
    LOTE_ACTUAL.remove();
  }
}
