package co.edu.javeriana.husi.model.enums;

/**
 * Módulo de la plataforma donde ocurrió el cambio. {@code etiqueta} se guarda en
 * {@code entidad_modificada}; {@code femenino} concuerda el resumen de una importación
 * («Universidades creadas», «Residentes creados»).
 */
public enum ModuloAuditoria {
  UNIVERSIDADES("Universidades", true),
  PROGRAMAS("Programas", false),
  RESIDENTES("Residentes", false),
  SERVICIOS("Servicios", false),
  ASIGNATURAS("Asignaturas", true),
  COORDINADORES("Coordinadores", false),
  USUARIOS("Usuarios", false),
  ROTACIONES("Rotaciones", true),
  ASIGNACIONES("Asignaciones de residentes", true),
  IMPORTACIONES("Importaciones", true);

  private final String etiqueta;
  private final boolean femenino;

  ModuloAuditoria(String etiqueta, boolean femenino) {
    this.etiqueta = etiqueta;
    this.femenino = femenino;
  }

  public String etiqueta() {
    return etiqueta;
  }

  public boolean esFemenino() {
    return femenino;
  }
}
