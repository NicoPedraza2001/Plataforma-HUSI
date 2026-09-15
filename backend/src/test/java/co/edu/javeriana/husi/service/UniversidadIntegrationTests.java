package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.exception.NegocioException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UniversidadIntegrationTests {

  @Autowired
  UniversidadService universidades;

  @Autowired
  ProgramaService programas;

  @Autowired
  ConsultaService consultas;

  @Autowired
  JdbcTemplate jdbc;

  private UniversidadRequest datos(String nombre) {
    return new UniversidadRequest(nombre, "Colombia", null);
  }

  @Test
  void crearEditarConsultarPersisteCamposYConservaEstado() {
    String nombre = "UNI-" + UUID.randomUUID();
    var u = universidades.crear(
      new UniversidadRequest(" " + nombre + " ", " Colombia ", " Docencia ")
    );
    assertThat(u.nombre()).isEqualTo(nombre);
    assertThat(u.pais()).isEqualTo("Colombia");
    assertThat(u.perfil()).isEqualTo("Docencia");
    assertThat(
      jdbc.queryForObject(
        "select nombre_universidad from universidad where id_universidad=?",
        String.class,
        u.id()
      )
    ).isEqualTo(nombre);
    universidades.cambiarEstado(u.id(), "Inactivo");
    var editada = universidades.actualizar(
      u.id(),
      new UniversidadRequest(nombre + " editada", "Ecuador", " ")
    );
    assertThat(editada.estado()).isEqualTo("Inactivo");
    assertThat(editada.perfil()).isNull();
    assertThat(universidades.consultar(u.id()).pais()).isEqualTo("Ecuador");
    assertThat(
      consultas
        .listar("universidades", Map.of("q", nombre, "estado", "Inactivo"))
        .totalElementos()
    ).isEqualTo(1);
  }

  @Test
  void rechazaCamposInvalidosDuplicadosYEstadosDesconocidos() {
    String nombre = "UNI-" + UUID.randomUUID();
    var u = universidades.crear(datos(nombre));
    universidades.cambiarEstado(u.id(), "Inactivo");
    assertThatThrownBy(() ->
      universidades.crear(datos(" " + nombre.toLowerCase() + " "))
    )
      .isInstanceOf(NegocioException.class)
      .hasMessageContaining("Ya existe");
    assertThatThrownBy(() ->
      universidades.crear(datos(" "))
    ).hasMessageContaining("obligatorio");
    assertThatThrownBy(() ->
      universidades.crear(new UniversidadRequest("Nombre", " ", null))
    ).hasMessageContaining("obligatorio");
    assertThatThrownBy(() ->
      universidades.crear(
        new UniversidadRequest("Nombre", "Colombia", "x".repeat(51))
      )
    ).hasMessageContaining("50");
    assertThatThrownBy(() ->
      universidades.cambiarEstado(u.id(), "Eliminado")
    ).hasMessageContaining("Activo o Inactivo");
    assertThatThrownBy(() ->
      universidades.consultar(Integer.MAX_VALUE)
    ).hasMessageContaining("No se encontró");
  }

  @Test
  void desactivarNoBorraNiCambiaLosProgramasYResidentes() {
    var u = universidades.crear(datos("UNI-" + UUID.randomUUID()));
    var p = programas.crear(
      new ProgramaRequest("Programa conservación", u.id())
    );
    jdbc.update(
      "insert into residente(id_programa,nombre_completo,numero_documento) values(?,?,?)",
      p.id(),
      "Prueba universidad",
      UUID.randomUUID().toString()
    );
    universidades.cambiarEstado(u.id(), "Inactivo");
    assertThat(universidades.catalogo()).noneMatch(c -> c.id().equals(u.id()));
    assertThat(programas.consultar(p.id()).estado()).isEqualTo("Activo");
    assertThat(
      universidades.programas(u.id(), Map.of()).totalElementos()
    ).isEqualTo(1);
    assertThat(
      jdbc.queryForObject(
        "select count(*) from residente where id_programa=?",
        Integer.class,
        p.id()
      )
    ).isEqualTo(1);
    assertThatThrownBy(() ->
      programas.crear(new ProgramaRequest("Otro", u.id()))
    ).hasMessageContaining("inactiva");
    universidades.cambiarEstado(u.id(), "Activo");
    assertThat(
      programas.crear(new ProgramaRequest("Otro", u.id())).universidadId()
    ).isEqualTo(u.id());
  }

  @Test
  void filtraProgramasPorIdInclusoDespuesDeRenombrarUniversidad() {
    var a = universidades.crear(datos("UNI-A-" + UUID.randomUUID()));
    var b = universidades.crear(datos("UNI-B-" + UUID.randomUUID()));
    var pa = programas.crear(new ProgramaRequest("Programa A", a.id()));
    programas.crear(new ProgramaRequest("Programa B", a.id()));
    programas.crear(new ProgramaRequest("Programa externo", b.id()));
    universidades.actualizar(a.id(), datos(a.nombre() + " editada"));
    var pagina = universidades.programas(
      a.id(),
      Map.of("tamano", "1", "pagina", "2", "universidadId", b.id().toString())
    );
    assertThat(pagina.totalElementos()).isEqualTo(2);
    assertThat(pagina.pagina()).isEqualTo(2);
    assertThat(pagina.contenido()).allSatisfy(row ->
      assertThat(row.get("universidadId")).isEqualTo(a.id())
    );
    assertThat(
      universidades.programas(a.id(), Map.of("q", "externo")).totalElementos()
    ).isZero();
    assertThat(programas.consultar(pa.id()).universidad()).isEqualTo(
      a.nombre() + " editada"
    );
    assertThatThrownBy(() ->
      universidades.programas(Integer.MAX_VALUE, Map.of())
    ).hasMessageContaining("No se encontró");
  }
}
