package co.edu.javeriana.husi.repository.support;

import java.util.HashMap;
import java.util.Map;

/**
 * Arma condiciones opcionales de una consulta y guarda sus parámetros. Un filtro sin valor no
 * agrega nada, así cada consulta se escribe una sola vez sin ramas por filtro.
 */
public final class ParametrosSql {

  private final Map<String, Object> valores = new HashMap<>();

  /** « and condicion» enlazando {@code nombre}; cadena vacía si {@code valor} es null. */
  public String y(String condicion, String nombre, Object valor) {
    if (valor == null) return "";
    valores.put(nombre, valor);
    return " and " + condicion;
  }

  /** Parámetro que la consulta usa siempre, como los límites de una ventana de fechas. */
  public ParametrosSql con(String nombre, Object valor) {
    valores.put(nombre, valor);
    return this;
  }

  public Map<String, Object> valores() {
    return valores;
  }
}
