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
    int universidadId = universidad();
    var programaCreado = programas.crear(new ProgramaRequest("Programa prueba", universidadId));
    assertThat(programas.cambiarEstado(programaCreado.id(), "Inactivo").estado()).isEqualTo(
      "Inactivo"
    );
    assertThat(universidades.programas(universidadId, Map.of()).contenido()).anyMatch(
      fila -> fila.get("id").equals(programaCreado.id()) && fila.get("estado").equals("Inactivo")
    );
    assertThat(
      programas
        .actualizar(programaCreado.id(), new ProgramaRequest("Editado inactivo", universidadId))
        .estado()
    ).isEqualTo("Inactivo");
    assertThat(programas.cambiarEstado(programaCreado.id(), "Activo").estado()).isEqualTo(
      "Activo"
    );
    assertThatThrownBy(() ->
      programas.cambiarEstado(programaCreado.id(), "Eliminado")
    ).hasMessageContaining("Activo o Inactivo");
  }

  @Test
  void eliminaFisicamenteProgramaSinResidentesYUniversidadSinProgramas() {
    int universidadId = universidad();
    var programaCreado = programas.crear(new ProgramaRequest("Programa prueba", universidadId));
    programas.eliminarDefinitivamente(programaCreado.id());
    assertThat(
      jdbc.queryForObject(
        "select count(*) from programa where id_programa=?",
        Integer.class,
        programaCreado.id()
      )
    ).isZero();
    universidades.eliminarDefinitivamente(universidadId);
    assertThat(
      jdbc.queryForObject(
        "select count(*) from universidad where id_universidad=?",
        Integer.class,
        universidadId
      )
    ).isZero();
  }

  @Test
  void protegeProgramasInactivosAsociadosAUniversidad() {
    int universidadId = universidad();
    var programaCreado = programas.crear(new ProgramaRequest("Programa prueba", universidadId));
    programas.cambiarEstado(programaCreado.id(), "Inactivo");
    assertThatThrownBy(() ->
      universidades.eliminarDefinitivamente(universidadId)
    ).hasMessageContaining("programas asociados");
    assertThat(
      jdbc.queryForObject(
        "select count(*) from universidad where id_universidad=?",
        Integer.class,
        universidadId
      )
    ).isEqualTo(1);
  }

  @Test
  void protegeResidentesInactivosYNoFuerzaCascadas() {
    int universidadId = universidad();
    var programaCreado = programas.crear(new ProgramaRequest("Programa prueba", universidadId));
    jdbc.update(
      "insert into residente(id_programa,id_tipo_documento,nombre_completo,numero_documento,estado) values(?,1,'Prueba',?,'Inactivo')",
      programaCreado.id(),
      UUID.randomUUID().toString()
    );
    assertThatThrownBy(() ->
      programas.eliminarDefinitivamente(programaCreado.id())
    ).hasMessageContaining("residentes asociados");
    assertThat(
      jdbc.queryForObject(
        "select count(*) from programa where id_programa=?",
        Integer.class,
        programaCreado.id()
      )
    ).isEqualTo(1);
  }

  @Test
  void noReactivaProgramaSiUniversidadEstaInactiva() {
    int universidadId = universidad();
    var programaCreado = programas.crear(new ProgramaRequest("Programa prueba", universidadId));
    programas.cambiarEstado(programaCreado.id(), "Inactivo");
    universidades.cambiarEstado(universidadId, "Inactivo");
    assertThatThrownBy(() ->
      programas.cambiarEstado(programaCreado.id(), "Activo")
    ).hasMessageContaining("universidad está inactiva");
  }
}
