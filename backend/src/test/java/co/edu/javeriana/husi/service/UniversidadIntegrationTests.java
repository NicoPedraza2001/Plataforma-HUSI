package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.exception.NegocioException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
// Los cambios se firman en el historial con el usuario en sesión.
@WithUserDetails("admin@husi.com")
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
    return new UniversidadRequest(nombre, "Ecuador");
  }

  @Test
  void crearEditarConsultarPersisteCamposYConservaEstado() {
    String nombre = "UNI-" + UUID.randomUUID();
    var universidadCreada = universidades.crear(
      new UniversidadRequest(" " + nombre + " ", " Colombia ")
    );
    assertThat(universidadCreada.nombre()).isEqualTo(nombre);
    assertThat(universidadCreada.pais()).isEqualTo("Colombia");
    assertThat(
      jdbc.queryForObject(
        "select nombre_universidad from universidad where id_universidad=?",
        String.class,
        universidadCreada.id()
      )
    ).isEqualTo(nombre);
    universidades.cambiarEstado(universidadCreada.id(), "Inactivo");
    var editada = universidades.actualizar(
      universidadCreada.id(),
      new UniversidadRequest(nombre + " editada", "Ecuador")
    );
    assertThat(editada.estado()).isEqualTo("Inactivo");
    assertThat(universidades.consultar(universidadCreada.id()).pais()).isEqualTo("Ecuador");
    assertThat(
      consultas
        .listar("universidades", Map.of("q", nombre, "estado", "Inactivo"))
        .totalElementos()
    ).isEqualTo(1);
  }

  @Test
  void rechazaCamposInvalidosDuplicadosYEstadosDesconocidos() {
    String nombre = "UNI-" + UUID.randomUUID();
    var universidadCreada = universidades.crear(datos(nombre));
    universidades.cambiarEstado(universidadCreada.id(), "Inactivo");
    assertThatThrownBy(() ->
      universidades.crear(datos(" " + nombre.toLowerCase() + " "))
    )
      .isInstanceOf(NegocioException.class)
      .hasMessageContaining("Ya existe");
    assertThatThrownBy(() ->
      universidades.crear(datos(" "))
    ).hasMessageContaining("obligatorio");
    assertThatThrownBy(() ->
      universidades.crear(new UniversidadRequest("Nombre", " "))
    ).hasMessageContaining("obligatorio");
    assertThatThrownBy(() ->
      universidades.cambiarEstado(universidadCreada.id(), "Eliminado")
    ).hasMessageContaining("Activo o Inactivo");
    assertThatThrownBy(() ->
      universidades.consultar(Integer.MAX_VALUE)
    ).hasMessageContaining("No se encontró");
  }

  @Test
  void desactivarNoBorraNiCambiaLosProgramasYResidentes() {
    var universidadCreada = universidades.crear(datos("UNI-" + UUID.randomUUID()));
    var programaCreado = programas.crear(
      new ProgramaRequest("Convenio Internacional", universidadCreada.id())
    );
    jdbc.update(
      "insert into residente(id_programa,id_tipo_documento,nombre_completo,numero_documento) values(?,1,?,?)",
      programaCreado.id(),
      "Prueba universidad",
      UUID.randomUUID().toString()
    );
    universidades.cambiarEstado(universidadCreada.id(), "Inactivo");
    assertThat(universidades.catalogo()).noneMatch(opcion -> opcion.id().equals(universidadCreada.id()));
    assertThat(programas.consultar(programaCreado.id()).estado()).isEqualTo("Activo");
    assertThat(
      universidades.programas(universidadCreada.id(), Map.of()).totalElementos()
    ).isEqualTo(1);
    assertThat(
      jdbc.queryForObject(
        "select count(*) from residente where id_programa=?",
        Integer.class,
        programaCreado.id()
      )
    ).isEqualTo(1);
    assertThatThrownBy(() ->
      programas.crear(new ProgramaRequest("Convenio Internacional", universidadCreada.id()))
    ).hasMessageContaining("inactiva");
    universidades.cambiarEstado(universidadCreada.id(), "Activo");
    assertThat(
      programas
        .actualizar(
          programaCreado.id(),
          new ProgramaRequest("Convenio Internacional", universidadCreada.id())
        )
        .universidadId()
    ).isEqualTo(universidadCreada.id());
  }

  @Test
  void filtraProgramasPorIdInclusoDespuesDeRenombrarUniversidad() {
    var universidadA = universidades.crear(datos("UNI-A-" + UUID.randomUUID()));
    var universidadB = universidades.crear(datos("UNI-B-" + UUID.randomUUID()));
    var pa = programas.crear(new ProgramaRequest("Convenio Internacional", universidadA.id()));
    // Programa anterior a la regla de convenios: solo se puede insertar directo en la base.
    jdbc.update(
      "insert into programa(id_universidad,nombre_programa) values(?,'Programa histórico')",
      universidadA.id()
    );
    programas.crear(new ProgramaRequest("Convenio Internacional", universidadB.id()));
    universidades.actualizar(universidadA.id(), datos(universidadA.nombre() + " editada"));
    var pagina = universidades.programas(
      universidadA.id(),
      Map.of("tamano", "1", "pagina", "2", "universidadId", universidadB.id().toString())
    );
    assertThat(pagina.totalElementos()).isEqualTo(2);
    assertThat(pagina.pagina()).isEqualTo(2);
    assertThat(pagina.contenido()).allSatisfy(row ->
      assertThat(row.get("universidadId")).isEqualTo(universidadA.id())
    );
    assertThat(
      universidades.programas(universidadA.id(), Map.of("q", "externo")).totalElementos()
    ).isZero();
    assertThat(programas.consultar(pa.id()).universidad()).isEqualTo(
      universidadA.nombre() + " editada"
    );
    assertThatThrownBy(() ->
      universidades.programas(Integer.MAX_VALUE, Map.of())
    ).hasMessageContaining("No se encontró");
  }
}
