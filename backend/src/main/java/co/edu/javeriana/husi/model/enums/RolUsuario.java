package co.edu.javeriana.husi.model.enums;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Roles con acceso a la plataforma. {@code nombreEnBaseDeDatos} es el valor de
 * {@code rol.nombre_rol}; el nombre del enum se usa como autoridad de Spring Security
 * ({@code ROLE_ADMINISTRADOR}, {@code ROLE_COORDINADOR}).
 */
public enum RolUsuario {
  ADMINISTRADOR("Administrador"),
  COORDINADOR("Coordinador");

  private final String nombreEnBaseDeDatos;

  RolUsuario(String nombreEnBaseDeDatos) {
    this.nombreEnBaseDeDatos = nombreEnBaseDeDatos;
  }

  public String nombreEnBaseDeDatos() {
    return nombreEnBaseDeDatos;
  }

  /** Busca el rol sin distinguir mayúsculas ni tildes; vacío si el rol no da acceso. */
  public static Optional<RolUsuario> desdeNombreEnBaseDeDatos(String nombreRol) {
    if (nombreRol == null) return Optional.empty();
    String buscado = normalizar(nombreRol);
    return Arrays.stream(values())
      .filter(rol -> normalizar(rol.nombreEnBaseDeDatos).equals(buscado))
      .findFirst();
  }

  private static String normalizar(String texto) {
    return Normalizer.normalize(texto.strip(), Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .toLowerCase(Locale.ROOT);
  }
}
