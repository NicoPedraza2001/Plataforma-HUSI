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

  private final EntityManager em;

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public List<Map<String, Object>> mapas(NativeQuery query) {
    query.setTupleTransformer((tuple, aliases) -> {
      Map<String, Object> row = new LinkedHashMap<>();
      for (int i = 0; i < aliases.length; i++) row.put(
        aliases[i],
        tuple[i] instanceof java.sql.Date d ? d.toString() : tuple[i]
      );
      return row;
    });
    return query.getResultList();
  }

  public List<Map<String, Object>> consultar(
    String sql,
    Map<String, Object> params
  ) {
    var query = em.createNativeQuery(sql);
    params.forEach(query::setParameter);
    return mapas(query.unwrap(NativeQuery.class));
  }

  public Object escalar(String sql, Map<String, Object> params) {
    var query = em.createNativeQuery(sql);
    params.forEach(query::setParameter);
    return query.getSingleResult();
  }
}
