package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.repository.ProgramaRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
// Los cambios se firman en el historial con el usuario en sesión.
@WithUserDetails("admin@husi.com")
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
      new ProgramaRequest("  convenio nacional  ", universidadId)
    );
    assertThat(creado.nombre()).isEqualTo("Convenio Nacional");
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
      new ProgramaRequest(" CONVENIO NACIONAL ", universidadId)
    );
    assertThat(programas.consultar(creado.id()).nombre()).isEqualTo(
      "Convenio Nacional"
    );
    programas.desactivar(creado.id());
    assertThat(repository.findById(creado.id())).isPresent();
    assertThat(programas.consultar(creado.id()).estado()).isEqualTo("Inactivo");
    assertThat(programas.listar()).noneMatch(programa -> programa.id().equals(creado.id()));
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
    var programaCreado = programas.crear(new ProgramaRequest("Convenio Nacional", universidadId));
    programas.desactivar(programaCreado.id());
    assertThatThrownBy(() ->
      programas.crear(new ProgramaRequest(" CONVENIO NACIONAL ", universidadId))
    ).isInstanceOfSatisfying(NegocioException.class, error ->
      assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT)
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
  void universidadExternaSoloAdmiteConvenioNacionalOInternacional() {
    assertThatThrownBy(() ->
      programas.crear(new ProgramaRequest("Pediatría", universidadId))
    ).hasMessageContaining("Convenio Nacional");
    var lote = new ImportacionProgramasRequest(
      List.of(new ImportacionProgramasRequest.Fila(2, "Pediatría", universidad))
    );
    assertThat(importacion.procesar(lote, false).errores()).isEqualTo(1);
  }

  @Test
  void universidadNacionalNoAdmiteConvenioInternacional() {
    String universidadNacional = "TEST-NACIONAL-" + UUID.randomUUID();
    Integer universidadNacionalId = jdbc.queryForObject(
      "INSERT INTO universidad(nombre_universidad,pais,estado) VALUES (?, 'colombia', 'Activo') RETURNING id_universidad",
      Integer.class,
      universidadNacional
    );
    assertThatThrownBy(() ->
      programas.crear(new ProgramaRequest("Convenio Internacional", universidadNacionalId))
    ).hasMessageContaining("es nacional");
    var convenioNacional = programas.crear(
      new ProgramaRequest("Convenio Nacional", universidadNacionalId)
    );
    assertThatThrownBy(() ->
      programas.actualizar(
        convenioNacional.id(),
        new ProgramaRequest("Convenio Internacional", universidadNacionalId)
      )
    ).hasMessageContaining("no puede ser internacional");
    var lote = new ImportacionProgramasRequest(
      List.of(
        new ImportacionProgramasRequest.Fila(2, "Convenio Internacional", universidadNacional)
      )
    );
    assertThat(importacion.procesar(lote, false).errores()).isEqualTo(1);
  }

  @Test
  void universidadInternacionalNoAdmiteConvenioNacional() {
    Integer universidadInternacionalId = jdbc.queryForObject(
      "INSERT INTO universidad(nombre_universidad,pais,estado) VALUES (?, 'españa', 'Activo') RETURNING id_universidad",
      Integer.class,
      "TEST-INTERNACIONAL-" + UUID.randomUUID()
    );
    assertThatThrownBy(() ->
      programas.crear(new ProgramaRequest("Convenio Nacional", universidadInternacionalId))
    ).hasMessageContaining("no puede ser nacional");
    assertThat(
      programas
        .crear(new ProgramaRequest("Convenio Internacional", universidadInternacionalId))
        .nombre()
    ).isEqualTo("Convenio Internacional");
  }

  @Test
  void validacionNoGuardaYCargaMixtaIdentificaCadaFila() {
    String universidadInternacional = "TEST-EXT-" + UUID.randomUUID();
    jdbc.update(
      "INSERT INTO universidad(nombre_universidad,pais,estado) VALUES (?, 'Francia', 'Activo')",
      universidadInternacional
    );
    var lote = new ImportacionProgramasRequest(
      List.of(
        new ImportacionProgramasRequest.Fila(2, "Convenio Nacional", universidad),
        new ImportacionProgramasRequest.Fila(3, "convenio nacional", universidad),
        new ImportacionProgramasRequest.Fila(4, "", universidad),
        new ImportacionProgramasRequest.Fila(
          5,
          "Convenio Nacional",
          "Universidad inexistente de prueba"
        ),
        new ImportacionProgramasRequest.Fila(
          6,
          "Convenio Internacional",
          universidadInternacional
        )
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
      .extracting(fila -> fila.fila())
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
