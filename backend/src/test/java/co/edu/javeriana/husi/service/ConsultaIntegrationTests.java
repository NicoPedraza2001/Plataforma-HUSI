package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.exception.NegocioException;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ConsultaIntegrationTests {

  @Autowired
  ConsultaService consultas;

  @Autowired
  DashboardService dashboard;

  @Autowired
  EntityManager em;

  @Test
  void buscaEnTodaLaBaseYPaginaSinDuplicar() {
    String prefijo = "Consulta-" + UUID.randomUUID();
    int universidad = (
      (Number) em
        .createNativeQuery(
          "insert into universidad(nombre_universidad,pais) values(:nombre,'Colombia') returning id_universidad"
        )
        .setParameter("nombre", prefijo)
        .getSingleResult()
    ).intValue();
    for (int i = 0; i < 12; i++) em.createNativeQuery(
      "insert into programa(id_universidad,nombre_programa) values(:id,:nombre)"
    )
      .setParameter("id", universidad)
      .setParameter("nombre", prefijo + String.format("-%02d", i))
      .executeUpdate();
    var primera = consultas.listar(
      "programas",
      Map.of("q", prefijo, "tamano", "5")
    );
    var segunda = consultas.listar(
      "programas",
      Map.of("q", prefijo, "tamano", "5", "pagina", "2")
    );
    assertThat(primera.totalElementos()).isEqualTo(12);
    assertThat(primera.contenido()).hasSize(5);
    assertThat(segunda.contenido()).doesNotContainAnyElementsOf(
      primera.contenido()
    );
    var busqueda = consultas.listar(
      "programas",
      Map.of("q", prefijo.toUpperCase() + "-11")
    );
    assertThat(busqueda.totalElementos()).isEqualTo(1);
    assertThat(
      consultas
        .listar("programas", Map.of("q", prefijo, "universidad", "otra"))
        .totalElementos()
    ).isZero();
    assertThat(
      consultas
        .listar("programas", Map.of("q", prefijo + "%_"))
        .totalElementos()
    ).isZero();
    assertThat(
      consultas
        .listar(
          "programas",
          Map.of("q", prefijo, "tamano", "5", "pagina", "999")
        )
        .pagina()
    ).isEqualTo(3);
  }

  @Test
  void validaParametrosYNoInterpolaSql() {
    assertThatThrownBy(() ->
      consultas.listar("residentes", Map.of("tamano", "101"))
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() ->
      consultas.listar("residentes", Map.of("pagina", "0"))
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() ->
      consultas.listar("residentes", Map.of("periodo", "2026-9"))
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() ->
      consultas.listar(
        "historial",
        Map.of("desde", "2026-09-02", "hasta", "2026-09-01")
      )
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() ->
      consultas.listar("usuarios", Map.of("contrasena", "x"))
    ).isInstanceOf(NegocioException.class);
    assertThat(
      consultas.listar("usuarios", Map.of("q", "' OR 1=1 --")).totalElementos()
    ).isZero();
  }

  @Test
  void todosLosDominiosDevuelvenProyeccionesSinCredenciales() {
    for (String dominio : List.of(
      "programas",
      "residentes",
      "universidades",
      "servicios",
      "usuarios",
      "historial",
      "rotaciones"
    )) {
      var pagina = consultas.listar(dominio, Map.of("tamano", "2"));
      assertThat(pagina.contenido().size()).isLessThanOrEqualTo(2);
      pagina
        .contenido()
        .forEach(row -> assertThat(row).doesNotContainKey("contrasena"));
    }
    assertThat(consultas.catalogos()).containsKeys(
      "universidades",
      "programas",
      "periodos",
      "roles"
    );
    assertThat(dashboard.consultar(Map.of())).containsKeys(
      "indicadores",
      "rotacionesPorServicio",
      "ocupacionPorServicio"
    );
  }

  @Test
  void fechaHistorialSeInterpretaEnBogotaYEsInclusiva() {
    int usuario = (
      (Number) em
        .createNativeQuery("select min(id_usuario) from usuario")
        .getSingleResult()
    ).intValue();
    String marca = UUID.randomUUID().toString();
    em.createNativeQuery(
      "insert into historial_auditoria(id_usuario,accion_realizada,entidad_modificada,fecha,detalle) values(:id,'PRUEBA','Pruebas','2026-08-02T04:59:00Z',:marca)"
    )
      .setParameter("id", usuario)
      .setParameter("marca", marca)
      .executeUpdate();
    assertThat(
      consultas
        .listar(
          "historial",
          Map.of("q", marca, "desde", "2026-08-01", "hasta", "2026-08-01")
        )
        .totalElementos()
    ).isEqualTo(1);
    assertThat(
      consultas
        .listar("historial", Map.of("q", marca, "desde", "2026-08-02"))
        .totalElementos()
    ).isZero();
  }
}
