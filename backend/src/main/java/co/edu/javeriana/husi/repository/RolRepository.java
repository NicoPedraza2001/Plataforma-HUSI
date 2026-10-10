package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.dto.response.CatalogoResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RolRepository {

  private final JdbcTemplate jdbc;

  public boolean esCoordinador(Integer id) {
    return Boolean.TRUE.equals(
      jdbc.queryForObject(
        "select exists(select 1 from rol where id_rol=? and lower(nombre_rol)='coordinador')",
        Boolean.class,
        id
      )
    );
  }

  public boolean existe(Integer id) {
    return Boolean.TRUE.equals(
      jdbc.queryForObject(
        "select exists(select 1 from rol where id_rol=?)",
        Boolean.class,
        id
      )
    );
  }

  public String consultarNombre(Integer id) {
    return jdbc
      .query("select nombre_rol from rol where id_rol=?", (resultado, numeroFila) -> resultado.getString(1), id)
      .stream()
      .findFirst()
      .orElse("—");
  }

  public List<CatalogoResponse> listar() {
    return jdbc.query(
      "select id_rol,nombre_rol from rol order by nombre_rol",
      (resultado, numeroFila) ->
        new CatalogoResponse(resultado.getInt(1), resultado.getString(2))
    );
  }
}
