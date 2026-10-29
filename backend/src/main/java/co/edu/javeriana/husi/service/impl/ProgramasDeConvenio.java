package co.edu.javeriana.husi.service.impl;

import java.util.*;

/**
 * Regla de negocio de los programas: solo la Pontificia Universidad Javeriana registra
 * programas académicos con nombre propio. Las demás universidades reciben residentes por
 * "Convenio Nacional" (universidades colombianas) o "Convenio Internacional" (del exterior),
 * y el convenio siempre debe corresponder al país de la universidad.
 */
final class ProgramasDeConvenio {

  static final String UNIVERSIDAD_PROPIA = "Pontificia Universidad Javeriana";
  static final String PAIS_NACIONAL = "Colombia";
  static final String CONVENIO_NACIONAL = "Convenio Nacional";
  static final String CONVENIO_INTERNACIONAL = "Convenio Internacional";

  /**
   * Nombre que ve el usuario para cada convenio. Es solo de presentación: lo que se guarda en
   * programa.nombre_programa y las comparaciones de esta clase siguen usando el nombre de arriba.
   */
  static final String ETIQUETA_CONVENIO_NACIONAL = "Movilidad Nacional";
  static final String ETIQUETA_CONVENIO_INTERNACIONAL = "Observador Internacional";

  static final String MENSAJE_SOLO_CONVENIOS =
    "Las universidades externas a la " +
    UNIVERSIDAD_PROPIA +
    " solo admiten los programas «" +
    ETIQUETA_CONVENIO_NACIONAL +
    "» o «" +
    ETIQUETA_CONVENIO_INTERNACIONAL +
    "».";

  private ProgramasDeConvenio() {}

  static boolean esUniversidadPropia(String nombreUniversidad) {
    return (
      nombreUniversidad != null &&
      UNIVERSIDAD_PROPIA.equalsIgnoreCase(nombreUniversidad.strip())
    );
  }

  /** Universidad de Colombia, sin importar mayúsculas ni espacios ("Colombia", "colombia"). */
  static boolean esPaisNacional(String pais) {
    return pais != null && PAIS_NACIONAL.equalsIgnoreCase(pais.strip());
  }

  /** Convenio que le corresponde a una universidad externa según su país. */
  static String convenioSegunPais(String pais) {
    return esPaisNacional(pais) ? CONVENIO_NACIONAL : CONVENIO_INTERNACIONAL;
  }

  /**
   * Medida de seguridad ante errores de digitación: una universidad de Colombia solo admite
   * "Convenio Nacional" y una del exterior solo "Convenio Internacional". Devuelve el mensaje
   * de rechazo, o vacío si el convenio corresponde al país.
   */
  static Optional<String> buscarErrorDeConvenioSegunPais(
    String convenio,
    String nombreUniversidad,
    String pais
  ) {
    Optional<String> convenioEscrito = buscarConvenio(convenio);
    String convenioEsperado = convenioSegunPais(pais);
    if (convenioEscrito.isEmpty() || convenioEscrito.get().equals(convenioEsperado)) {
      return Optional.empty();
    }
    String paisMostrado = pais == null || pais.isBlank() ? "sin país" : pais.strip();
    return Optional.of(
      esPaisNacional(pais)
        ? "La universidad «" +
          nombreUniversidad +
          "» es nacional (" +
          paisMostrado +
          "), por ende su convenio no puede ser internacional. Usa «" +
          ETIQUETA_CONVENIO_NACIONAL +
          "»."
        : "La universidad «" +
          nombreUniversidad +
          "» es internacional (" +
          paisMostrado +
          "), por ende su convenio no puede ser nacional. Usa «" +
          ETIQUETA_CONVENIO_INTERNACIONAL +
          "»."
    );
  }

  /** Nombre del convenio tal como se guarda, o vacío si el texto no es ninguno de los dos. */
  static Optional<String> buscarConvenio(String nombrePrograma) {
    String nombreLimpio = nombrePrograma == null ? "" : nombrePrograma.strip();
    return List.of(CONVENIO_NACIONAL, CONVENIO_INTERNACIONAL)
      .stream()
      .filter(convenio -> convenio.equalsIgnoreCase(nombreLimpio))
      .findFirst();
  }
}
