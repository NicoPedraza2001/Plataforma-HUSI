package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.dto.request.CargaInicialRequest;
import co.edu.javeriana.husi.dto.request.ImportacionRotacionesRequest;
import co.edu.javeriana.husi.service.CargaInicialService;
import co.edu.javeriana.husi.service.ImportacionRotacionesService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/** CU-09 Importar desde Excel, parte del servidor (RF-18 a RF-20). */
class CU09ImportacionIntegrationTests extends BaseCasoDeUso {

  @Autowired
  CargaInicialService cargaInicial;

  @Autowired
  ImportacionRotacionesService importacionRotaciones;

  private CargaInicialRequest.Fila filaConFechas(String inicio, String fin) {
    String marca = UUID.randomUUID().toString();
    return new CargaInicialRequest.Fila(
      2,
      "Universidad " + marca,
      "Colombia",
      "Convenio Nacional",
      "Residente " + marca,
      "CC",
      marca,
      "",
      "Servicio " + marca,
      "2",
      "2",
      "Asignatura " + marca,
      inicio,
      fin
    );
  }

  private CargaInicialRequest solicitud(List<CargaInicialRequest.Fila> filas, String validacion) {
    return new CargaInicialRequest(filas, true, validacion);
  }

  private ImportacionRotacionesRequest.Fila filaDeRotacion(int numeroDeFila) {
    return new ImportacionRotacionesRequest.Fila(
      numeroDeFila,
      "Asignatura",
      "Servicio",
      "2091-01-01",
      "2091-01-31"
    );
  }

  @Test
  @DisplayName("CU09-FA1 La importación admite periodos históricos, a diferencia de la creación manual")
  void laImportacionAdmitePeriodosHistoricos() {
    var fila = filaConFechas("2020-03-01", "2020-03-31");

    var revision = cargaInicial.validar(solicitud(List.of(fila), null));
    assertThat(revision.errores()).as(revision.toString()).isZero();
    var resultado = cargaInicial.confirmar(solicitud(List.of(fila), revision.validacion()));

    assertThat(resultado.guardado()).isTrue();
    Integer asignaturaImportada = jdbc.queryForObject(
      "select id_asignatura from asignatura where nombre_asignatura=?",
      Integer.class,
      fila.asignatura()
    );
    assertThat(
      jdbc.queryForObject(
        "select count(*) from rotacion where id_asignatura=? and fecha_inicio=?",
        Integer.class,
        asignaturaImportada,
        LocalDate.of(2020, 3, 1)
      )
    ).isEqualTo(1);
    debeFallar(HttpStatus.BAD_REQUEST, "no puede ser anterior a hoy", () ->
      crearRotacion(asignaturaImportada, LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30))
    );
  }

  @Test
  @DisplayName("CU09-EX1 Cero filas o más de 5000 responden 400")
  void ceroFilasOMasDe5000Responden400() {
    debeFallar(HttpStatus.BAD_REQUEST, "entre 1 y 5000", () ->
      cargaInicial.validar(solicitud(List.of(), null))
    );
    var demasiadas = Collections.nCopies(5001, filaConFechas("2091-01-01", "2091-01-31"));
    debeFallar(HttpStatus.BAD_REQUEST, "entre 1 y 5000", () ->
      cargaInicial.validar(solicitud(demasiadas, null))
    );
  }

  @Test
  @DisplayName("CU09-EX2 Confirmar sin una revisión vigente responde 409 y no guarda")
  void confirmarSinRevisionResponde409() {
    var fila = filaConFechas("2091-01-01", "2091-01-31");

    debeFallar(HttpStatus.CONFLICT, "Vuelve a validar", () ->
      cargaInicial.confirmar(solicitud(List.of(fila), null))
    );

    assertThat(
      jdbc.queryForObject(
        "select count(*) from residente where numero_documento=?",
        Integer.class,
        fila.documento()
      )
    ).isZero();
  }

  @Test
  @DisplayName("CU09-EX3 Confirmar sin ninguna fila válida responde 400")
  void confirmarSinFilasValidasResponde400() {
    var filaInvalida = filaConFechas("no-es-una-fecha", "2091-01-31");
    var revision = cargaInicial.validar(solicitud(List.of(filaInvalida), null));
    assertThat(revision.errores()).isEqualTo(1);

    debeFallar(HttpStatus.BAD_REQUEST, "No hay filas válidas", () ->
      cargaInicial.confirmar(solicitud(List.of(filaInvalida), revision.validacion()))
    );
  }

  @Test
  @DisplayName("CU09-EX9 Un identificador de lote inválido responde 400")
  void unLoteInvalidoResponde400() {
    debeFallar(HttpStatus.BAD_REQUEST, "no es válido", () ->
      historial.cancelarImportacion("no-es-un-lote")
    );
    debeFallar(HttpStatus.BAD_REQUEST, "no es válido", () -> historial.cancelarImportacion(null));
  }

  @Test
  @DisplayName("CU09-EX10 La importación de rotaciones rechaza 0 o más de 1000 filas")
  void laImportacionDeRotacionesLimitaLasFilas() {
    debeFallar(HttpStatus.BAD_REQUEST, "entre 1 y 1000", () ->
      importacionRotaciones.procesar(new ImportacionRotacionesRequest(List.of()), false)
    );
    var demasiadas = java.util.stream.IntStream.rangeClosed(1, 1001)
      .mapToObj(this::filaDeRotacion)
      .toList();
    debeFallar(HttpStatus.BAD_REQUEST, "entre 1 y 1000", () ->
      importacionRotaciones.procesar(new ImportacionRotacionesRequest(demasiadas), false)
    );
  }

  @Test
  @DisplayName("CU09-EX10 La importación de rotaciones rechaza números de fila repetidos o negativos")
  void laImportacionDeRotacionesRechazaNumerosDeFilaInvalidos() {
    debeFallar(HttpStatus.BAD_REQUEST, "positivos y distintos", () ->
      importacionRotaciones.procesar(
        new ImportacionRotacionesRequest(List.of(filaDeRotacion(2), filaDeRotacion(2))),
        false
      )
    );
    debeFallar(HttpStatus.BAD_REQUEST, "positivos y distintos", () ->
      importacionRotaciones.procesar(
        new ImportacionRotacionesRequest(List.of(filaDeRotacion(-1))),
        false
      )
    );
  }
}
