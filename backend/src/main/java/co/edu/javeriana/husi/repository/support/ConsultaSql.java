package co.edu.javeriana.husi.repository.support;

import jakarta.persistence.EntityManager;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

/** Ejecuta SQL interno parametrizado. No acepta sentencias suministradas por el cliente. */
@Repository
@RequiredArgsConstructor
public class ConsultaSql {

  private final EntityManager entityManager;

  @SuppressWarnings({ "unchecked", "rawtypes" })
  /** Ejecuta la consulta y devuelve cada fila como un mapa alias → valor (fechas como texto). */
  public List<Map<String, Object>> mapas(NativeQuery query) {
    query.setTupleTransformer((valores, alias) -> {
      Map<String, Object> fila = new LinkedHashMap<>();
      for (int i = 0; i < alias.length; i++) fila.put(
        alias[i],
        valores[i] instanceof java.sql.Date fecha ? fecha.toString() : valores[i]
      );
      return fila;
    });
    return query.getResultList();
  }

  public List<Map<String, Object>> consultar(
    String sql,
    Map<String, Object> parametros
  ) {
    var query = entityManager.createNativeQuery(sql);
    parametros.forEach(query::setParameter);
    return mapas(query.unwrap(NativeQuery.class));
  }

  /** Ejecuta una consulta que devuelve un único valor (por ejemplo, un count). */
  public Object escalar(String sql, Map<String, Object> parametros) {
    var query = entityManager.createNativeQuery(sql);
    parametros.forEach(query::setParameter);
    return query.getSingleResult();
  }
}
