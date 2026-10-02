package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.repository.ProgramaRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProgramaIntegrationTests {

  @Autowired
  ProgramaService programas;

  @Autowired
  ImportacionProgramasService importacion;

  @Autowired
  ProgramaRepository repository;

  @Autowired
  JdbcTemplate jdbc;

  Integer universidadId;
  String universidad;

  @BeforeEach
  void preparar() {
    universidad = "TEST-" + UUID.randomUUID();
    universidadId = jdbc.queryForObject(
      "INSERT INTO universidad(nombre_universidad,pais,estado) VALUES (?, 'Colombia', 'Activo') RETURNING id_universidad",
      Integer.class,
      universidad
    );
  }

  @Test
  void crearConsultarActualizarYDesactivarConservaElRegistroYResidentes() {
    var creado = programas.crear(
      new ProgramaRequest("  Pediatría  ", universidadId)
    );
    assertThat(creado.nombre()).isEqualTo("Pediatría");
    assertThat(programas.consultar(creado.id()).universidad()).isEqualTo(
      universidad
    );
    jdbc.update(
      "INSERT INTO residente(id_programa,id_tipo_documento,nombre_completo,numero_documento) VALUES (?,1,?,?)",
      creado.id(),
      "Prueba transaccional",
      UUID.randomUUID().toString()
    );
    programas.actualizar(
      creado.id(),
      new ProgramaRequest("Pediatría actualizada", universidadId)
    );
    assertThat(programas.consultar(creado.id()).nombre()).isEqualTo(
      "Pediatría actualizada"
    );
    programas.desactivar(creado.id());
    assertThat(repository.findById(creado.id())).isPresent();
    assertThat(programas.consultar(creado.id()).estado()).isEqualTo("Inactivo");
    assertThat(programas.listar()).noneMatch(p -> p.id().equals(creado.id()));
    assertThat(
      jdbc.queryForObject(
        "SELECT count(*) FROM residente WHERE id_programa=?",
        Integer.class,
        creado.id()
      )
    ).isEqualTo(1);
  }

  @Test
  void rechazaDuplicadosInclusoInactivosYUniversidadesInvalidas() {
    var p = programas.crear(new ProgramaRequest("Neurología", universidadId));
    programas.desactivar(p.id());
    assertThatThrownBy(() ->
      programas.crear(new ProgramaRequest(" NEUROLOGÍA ", universidadId))
    ).isInstanceOfSatisfying(NegocioException.class, e ->
      assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT)
    );
    assertThatThrownBy(() ->
      programas.crear(new ProgramaRequest("Otro", Integer.MAX_VALUE))
    ).hasMessageContaining("universidad no existe");
    assertThatThrownBy(() ->
      programas.crear(new ProgramaRequest("  ", universidadId))
    ).hasMessageContaining("Falta el nombre");
    assertThatThrownBy(() ->
      programas.crear(new ProgramaRequest("x".repeat(151), universidadId))
    ).hasMessageContaining("150");
  }

  @Test
  void validacionNoGuardaYCargaMixtaIdentificaCadaFila() {
    var lote = new ImportacionProgramasRequest(
      List.of(
        new ImportacionProgramasRequest.Fila(2, "Programa A", universidad),
        new ImportacionProgramasRequest.Fila(3, "programa a", universidad),
        new ImportacionProgramasRequest.Fila(4, "", universidad),
        new ImportacionProgramasRequest.Fila(
          5,
          "Programa B",
          "Universidad inexistente de prueba"
        ),
        new ImportacionProgramasRequest.Fila(6, "Programa C", universidad)
      )
    );
    long antes = repository.count();
    var validacion = importacion.procesar(lote, false);
    assertThat(validacion.validos()).isEqualTo(2);
    assertThat(validacion.omitidos()).isEqualTo(1);
    assertThat(validacion.errores()).isEqualTo(2);
    assertThat(repository.count()).isEqualTo(antes);
    var resultado = importacion.procesar(lote, true);
    assertThat(resultado.creados()).isEqualTo(2);
    assertThat(resultado.omitidos()).isEqualTo(1);
    assertThat(resultado.errores()).isEqualTo(2);
    assertThat(resultado.filas())
      .extracting(f -> f.fila())
      .containsExactly(2, 3, 4, 5, 6);
    assertThat(repository.count()).isEqualTo(antes + 2);
    assertThat(importacion.procesar(lote, true).creados()).isZero();
  }

  @Test
  void universidadInactivaNoAceptaProgramasYFilasRepetidasSeRechazan() {
    jdbc.update(
      "UPDATE universidad SET estado='Inactivo' WHERE id_universidad=?",
      universidadId
    );
    assertThatThrownBy(() ->
      programas.crear(new ProgramaRequest("Programa", universidadId))
    ).hasMessageContaining("inactiva");
    var lote = new ImportacionProgramasRequest(
      List.of(
        new ImportacionProgramasRequest.Fila(2, "A", universidad),
        new ImportacionProgramasRequest.Fila(2, "B", universidad)
      )
    );
    assertThatThrownBy(() ->
      importacion.procesar(lote, true)
    ).hasMessageContaining("fila de Excel diferente");
  }
}
