package co.edu.javeriana.husi.model.enums;

/**
 * Acción que registra el historial de auditoría.
 *
 * <p>{@code etiqueta} es el texto que se guarda en {@code accion_realizada} y se muestra en
 * pantalla. {@code raizParticipio} arma el resumen de una importación: «Residentes creados»,
 * «Universidades activadas»…
 */
public enum AccionAuditoria {
  CREO("CREÓ", "cread"),
  ACTUALIZO("ACTUALIZÓ", "actualizad"),
  ACTIVO("ACTIVÓ", "activad"),
  INACTIVO("INACTIVÓ", "inactivad"),
  ELIMINO("ELIMINÓ", "eliminad"),
  ASIGNO("ASIGNÓ", "registrad"),
  CAMBIO_ESTADO("CAMBIÓ ESTADO", "modificad"),
  TRASLADO("TRASLADÓ", "trasladad"),
  IMPORTO("IMPORTÓ", "importad");

  private final String etiqueta;
  private final String raizParticipio;

  AccionAuditoria(String etiqueta, String raizParticipio) {
    this.etiqueta = etiqueta;
    this.raizParticipio = raizParticipio;
  }

  public String etiqueta() {
    return etiqueta;
  }

  /** Participio en plural concordando con el módulo: «creados» o «creadas». */
  public String participioPlural(boolean femenino) {
    return raizParticipio + (femenino ? "as" : "os");
  }

  /** ACTIVÓ o INACTIVÓ según el estado al que pasó el registro. */
  public static AccionAuditoria segunEstado(String estadoNuevo) {
    return "Activo".equals(estadoNuevo) ? ACTIVO : INACTIVO;
  }
}
