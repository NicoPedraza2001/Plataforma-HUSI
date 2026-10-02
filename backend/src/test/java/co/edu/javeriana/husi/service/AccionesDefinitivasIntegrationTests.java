package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AccionesDefinitivasIntegrationTests {

  @Autowired
  UniversidadService universidades;

  @Autowired
  ProgramaService programas;

  @Autowired
  ConsultaService consultas;

  @Autowired
  JdbcTemplate jdbc;

  private Integer universidad() {
    return universidades
      .crear(
        new UniversidadRequest(
          "ACCIONES-" + UUID.randomUUID(),
          "Colombia",
          null
        )
      )
      .id();
  }

  @Test
  void estadosSonReversiblesYProgramasInactivosSiguenEnElListado() {
    int u = universidad();
    var p = programas.crear(new ProgramaRequest("Programa prueba", u));
    assertThat(programas.cambiarEstado(p.id(), "Inactivo").estado()).isEqualTo(
      "Inactivo"
    );
    assertThat(universidades.programas(u, Map.of()).contenido()).anyMatch(
      f -> f.get("id").equals(p.id()) && f.get("estado").equals("Inactivo")
    );
    assertThat(
      programas
        .actualizar(p.id(), new ProgramaRequest("Editado inactivo", u))
        .estado()
    ).isEqualTo("Inactivo");
    assertThat(programas.cambiarEstado(p.id(), "Activo").estado()).isEqualTo(
      "Activo"
    );
    assertThatThrownBy(() ->
      programas.cambiarEstado(p.id(), "Eliminado")
    ).hasMessageContaining("Activo o Inactivo");
  }

  @Test
  void eliminaFisicamenteProgramaSinResidentesYUniversidadSinProgramas() {
    int u = universidad();
    var p = programas.crear(new ProgramaRequest("Programa prueba", u));
    programas.eliminarDefinitivamente(p.id());
    assertThat(
      jdbc.queryForObject(
        "select count(*) from programa where id_programa=?",
        Integer.class,
        p.id()
      )
    ).isZero();
    universidades.eliminarDefinitivamente(u);
    assertThat(
      jdbc.queryForObject(
        "select count(*) from universidad where id_universidad=?",
        Integer.class,
        u
      )
    ).isZero();
  }

  @Test
  void protegeProgramasInactivosAsociadosAUniversidad() {
    int u = universidad();
    var p = programas.crear(new ProgramaRequest("Programa prueba", u));
    programas.cambiarEstado(p.id(), "Inactivo");
    assertThatThrownBy(() ->
      universidades.eliminarDefinitivamente(u)
    ).hasMessageContaining("programas asociados");
    assertThat(
      jdbc.queryForObject(
        "select count(*) from universidad where id_universidad=?",
        Integer.class,
        u
      )
    ).isEqualTo(1);
  }

  @Test
  void protegeResidentesInactivosYNoFuerzaCascadas() {
    int u = universidad();
    var p = programas.crear(new ProgramaRequest("Programa prueba", u));
    jdbc.update(
      "insert into residente(id_programa,id_tipo_documento,nombre_completo,numero_documento,estado) values(?,1,'Prueba',?,'Inactivo')",
      p.id(),
      UUID.randomUUID().toString()
    );
    assertThatThrownBy(() ->
      programas.eliminarDefinitivamente(p.id())
    ).hasMessageContaining("residentes asociados");
    assertThat(
      jdbc.queryForObject(
        "select count(*) from programa where id_programa=?",
        Integer.class,
        p.id()
      )
    ).isEqualTo(1);
  }

  @Test
  void noReactivaProgramaSiUniversidadEstaInactiva() {
    int u = universidad();
    var p = programas.crear(new ProgramaRequest("Programa prueba", u));
    programas.cambiarEstado(p.id(), "Inactivo");
    universidades.cambiarEstado(u, "Inactivo");
    assertThatThrownBy(() ->
      programas.cambiarEstado(p.id(), "Activo")
    ).hasMessageContaining("universidad está inactiva");
  }
}
