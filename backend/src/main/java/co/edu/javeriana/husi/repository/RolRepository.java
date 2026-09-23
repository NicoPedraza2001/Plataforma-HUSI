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

  public boolean existe(Integer id) {
    return Boolean.TRUE.equals(
      jdbc.queryForObject(
        "select exists(select 1 from rol where id_rol=?)",
        Boolean.class,
        id
      )
    );
  }

  public List<CatalogoResponse> listar() {
    return jdbc.query(
      "select id_rol,nombre_rol from rol order by nombre_rol",
      (rs, n) -> new CatalogoResponse(rs.getInt(1), rs.getString(2))
    );
  }
}
