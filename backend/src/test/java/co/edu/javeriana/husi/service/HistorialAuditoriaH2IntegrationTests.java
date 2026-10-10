package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.util.ImportacionEnCurso;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
  properties = "spring.datasource.url=jdbc:h2:mem:historial;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
)
@ActiveProfiles("local")
@Transactional
class HistorialAuditoriaH2IntegrationTests {

  @Autowired
  UniversidadService universidades;

  @Autowired
  ImportacionProgramasService importacionProgramas;

  @Autowired
  HistorialAuditoriaService historial;

  @Autowired
  JdbcTemplate jdbc;

  private List<Map<String, Object>> movimientosDe(String registro) {
    return jdbc.queryForList(
      "select id_historial,accion_realizada,entidad_modificada,detalle from historial_auditoria where registro=? order by id_historial",
      registro
    );
  }

  @Test
  void registraCreacionYCambiosConDatosAnterioresYNuevos() {
    String nombre = "Universidad auditada " + UUID.randomUUID();
    var creada = universidades.crear(new UniversidadRequest(nombre, "Colombia"));
    // Guardar sin cambios no deja movimiento.
    universidades.actualizar(creada.id(), new UniversidadRequest(nombre, "Colombia"));
    universidades.actualizar(creada.id(), new UniversidadRequest(nombre, "Perú"));
    universidades.cambiarEstado(creada.id(), "Inactivo");

    var movimientos = movimientosDe(nombre);
    assertThat(movimientos)
      .extracting(movimiento -> movimiento.get("accion_realizada"))
      .containsExactly("CREÓ", "ACTUALIZÓ", "INACTIVÓ");
    assertThat(movimientos.get(1).get("detalle")).isEqualTo("País: Colombia → Perú");

    var detalle = historial.consultar((Integer) movimientos.get(1).get("id_historial"));
    assertThat(detalle.usuario()).isEqualTo("Administrador Sistema");
    assertThat(detalle.modulo()).isEqualTo("Universidades");
    assertThat(detalle.datosAnteriores()).containsEntry("País", "Colombia");
    assertThat(detalle.datosNuevos()).containsEntry("País", "Perú").containsEntry("Nombre", nombre);
    assertThat(detalle.esImportacion()).isFalse();
  }

  @Test
  void cancelarUnaImportacionConservaLoCreadoYNoAfectaLasConfirmadas() {
    String lote = UUID.randomUUID().toString();
    ImportacionEnCurso.iniciar(new ImportacionEnCurso.Lote(lote, "Carga inicial desde Excel", "prueba.xlsx"));
    try {
      universidades.crear(new UniversidadRequest("Universidad cancelada " + lote, "Colombia"));
    } finally {
      ImportacionEnCurso.terminar();
    }

    historial.cancelarImportacion(lote);
    var resumen = historial.consultar(
      jdbc.queryForObject(
        "select id_historial from historial_auditoria where lote_importacion=?",
        Integer.class,
        lote
      )
    );
    assertThat(resumen.datosNuevos())
      .containsEntry("Universidades creadas", 1)
      .containsEntry("Estado de la importación", "Cancelada");
    assertThat(resumen.detalle()).endsWith("Estado de la importación: Cancelada");

    // Un lote que no creó nada no deja fila; un identificador inválido se rechaza.
    historial.cancelarImportacion(UUID.randomUUID().toString());
    assertThatThrownBy(() -> historial.cancelarImportacion("no-es-un-lote")).isInstanceOf(
      NegocioException.class
    );
  }

  @Test
  void unaImportacionQuedaComoUnSoloMovimientoConSuResumen() {
    long antes = jdbc.queryForObject("select count(*) from historial_auditoria", Long.class);
    String universidad = "Pontificia Universidad Javeriana";
    jdbc.update(
      "insert into universidad(nombre_universidad,pais) values (?, 'Colombia')",
      universidad
    );
    var resultado = importacionProgramas.procesar(
      new ImportacionProgramasRequest(
        List.of(
          new ImportacionProgramasRequest.Fila(2, "Medicina interna auditada", universidad),
          new ImportacionProgramasRequest.Fila(3, "Pediatría auditada", universidad),
          new ImportacionProgramasRequest.Fila(4, "Pediatría auditada", universidad)
        )
      ),
      true
    );
    assertThat(resultado.creados()).isEqualTo(2);

    assertThat(
      jdbc.queryForObject("select count(*) from historial_auditoria", Long.class)
    ).isEqualTo(antes + 1);
    var resumen = historial.consultar(
      jdbc.queryForObject("select max(id_historial) from historial_auditoria", Integer.class)
    );
    assertThat(resumen.accion()).isEqualTo("IMPORTÓ");
    assertThat(resumen.esImportacion()).isTrue();
    assertThat(resumen.datosNuevos())
      .containsEntry("Programas creados", 2)
      .containsEntry("Filas del archivo", 3)
      .containsEntry("Filas omitidas", 1)
      .containsEntry("Estado de la importación", "Finalizada");
  }
}
