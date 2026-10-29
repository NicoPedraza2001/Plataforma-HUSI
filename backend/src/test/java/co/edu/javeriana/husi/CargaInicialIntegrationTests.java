package co.edu.javeriana.husi;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.CargaInicialRequest;
import co.edu.javeriana.husi.dto.request.CargaInicialRequest.Fila;
import co.edu.javeriana.husi.service.CargaInicialService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
// Los cambios se firman en el historial con el usuario en sesión.
@WithUserDetails("admin@husi.com")
class CargaInicialIntegrationTests {

  @Autowired
  CargaInicialService carga;

  @Autowired
  JdbcTemplate jdbc;

  private Fila fila(int numeroFila, String documento, String servicio, String cupos) {
    return new Fila(
      numeroFila,
      "Universidad importación",
      "Colombia",
      "Convenio Nacional",
      "Residente " + documento,
      "CC",
      documento,
      "",
      servicio,
      cupos,
      cupos,
      "Asignatura importación",
      "2097-01-01",
      "2097-01-31"
    );
  }

  private CargaInicialRequest request(List<Fila> filas) {
    return new CargaInicialRequest(filas, true, null);
  }

  @Test
  void validaSinEscribirYCreaTodaLaCadenaSinDuplicar() {
    var datos = request(
      List.of(
        fila(2, "IMP-1", "Servicio importación", "2"),
        fila(3, "IMP-2", "Servicio importación", "2")
      )
    );
    long antes = jdbc.queryForObject(
      "select count(*) from universidad",
      Long.class
    );
    var previa = carga.validar(datos);
    assertThat(previa.validos()).as(previa.toString()).isEqualTo(2);
    assertThat(previa.nuevos())
      .containsEntry("rotacion", 1)
      .containsEntry("residente", 2)
      .containsEntry("universidad", 1)
      .containsEntry("rotacion_residente", 2);
    assertThat(
      jdbc.queryForObject("select count(*) from universidad", Long.class)
    ).isEqualTo(antes);
    var resultado = carga.confirmar(
      new CargaInicialRequest(datos.filas(), true, previa.validacion())
    );
    assertThat(resultado.guardado()).isTrue();
    assertThat(
      jdbc.queryForObject(
        "select count(*) from rotacion_residente rr join residente r on r.id_residente=rr.id_residente join programa p on p.id_programa=r.id_programa join universidad u on u.id_universidad=p.id_universidad where u.nombre_universidad='Universidad importación' and rr.estado='Asignado'",
        Integer.class
      )
    ).isEqualTo(2);
    assertThat(
      jdbc.queryForObject(
        "select cupo_disponible from rotacion where fecha_inicio='2097-01-01'",
        Integer.class
      )
    ).isZero();
    assertThat(carga.validar(datos).omitidos()).isEqualTo(2);
  }

  @Test
  void respetaPrioridadYDejaEnEsperaLasFilasSinCupo() {
    var solicitud = request(
      List.of(
        fila(3, "IMP-B", "Servicio prioridad", "1"),
        fila(2, "IMP-A", "Servicio prioridad", "1"),
        fila(4, "IMP-B", "Servicio prioridad", "1")
      )
    );
    var resultado = carga.validar(solicitud);
    assertThat(resultado.validos()).isEqualTo(1);
    assertThat(resultado.espera()).isEqualTo(1);
    assertThat(resultado.omitidos()).isEqualTo(1);
    assertThat(resultado.filas().getFirst().fila()).isEqualTo(3);
    assertThat(resultado.filas().get(1).mensaje()).contains("lista de espera (posición 1)");
    var guardado = carga.confirmar(
      new CargaInicialRequest(solicitud.filas(), true, resultado.validacion())
    );
    assertThat(guardado.espera()).isEqualTo(1);
    assertThat(
      jdbc.queryForObject(
        "select rr.estado from rotacion_residente rr join residente r using(id_residente) where r.numero_documento='IMP-A'",
        String.class
      )
    ).isEqualTo("Esperando cupo");
  }

  @Test
  void rechazaCamposInvalidosSinEscribir() {
    var buenas = fila(2, "IMP-X", "Servicio errores", "1");
    var mala = new Fila(
      3,
      buenas.universidad(),
      "",
      buenas.programa(),
      buenas.residente(),
      "",
      buenas.documento(),
      "",
      buenas.servicio(),
      "1.5",
      "4",
      buenas.asignatura(),
      "2097-02-30",
      buenas.fin()
    );
    assertThat(carga.validar(request(List.of(mala))).errores()).isEqualTo(1);
    assertThat(
      carga
        .validar(new CargaInicialRequest(List.of(buenas), false, null))
        .errores()
    ).isEqualTo(1);
  }

  @Test
  void rechazaProgramasPropiosEnUniversidadesExternas() {
    var filaConConvenio = fila(2, "IMP-Z", "Servicio convenio", "1");
    var filaConProgramaPropio = new Fila(
      2,
      "Universidad externa sin convenio " + filaConConvenio.documento(),
      filaConConvenio.pais(),
      "Especialización en Pediatría",
      filaConConvenio.residente(),
      filaConConvenio.tipoDocumento(),
      filaConConvenio.documento(),
      "",
      filaConConvenio.servicio(),
      "1",
      "1",
      filaConConvenio.asignatura(),
      filaConConvenio.inicio(),
      filaConConvenio.fin()
    );
    var resultado = carga.validar(request(List.of(filaConProgramaPropio)));
    assertThat(resultado.errores()).isEqualTo(1);
    assertThat(resultado.filas().getFirst().mensaje()).contains("Movilidad Nacional");
  }

  @Test
  void rechazaConvenioInternacionalEnUniversidadNacional() {
    var filaNacional = fila(2, "IMP-N", "Servicio convenio nacional", "1");
    var filaConConvenioInternacional = new Fila(
      2,
      "Universidad nacional " + UUID.randomUUID(),
      "colombia",
      "Convenio Internacional",
      filaNacional.residente(),
      filaNacional.tipoDocumento(),
      filaNacional.documento(),
      "",
      filaNacional.servicio(),
      "1",
      "1",
      filaNacional.asignatura(),
      filaNacional.inicio(),
      filaNacional.fin()
    );
    var resultado = carga.validar(request(List.of(filaConConvenioInternacional)));
    assertThat(resultado.errores()).isEqualTo(1);
    assertThat(resultado.filas().getFirst().mensaje()).contains("es nacional");
  }

  @Test
  void rechazaConvenioNacionalEnUniversidadInternacional() {
    var filaBase = fila(2, "IMP-I", "Servicio convenio internacional", "1");
    var filaConConvenioNacional = new Fila(
      2,
      "Universidad internacional " + UUID.randomUUID(),
      "España",
      "Convenio Nacional",
      filaBase.residente(),
      filaBase.tipoDocumento(),
      filaBase.documento(),
      "",
      filaBase.servicio(),
      "1",
      "1",
      filaBase.asignatura(),
      filaBase.inicio(),
      filaBase.fin()
    );
    var resultado = carga.validar(request(List.of(filaConConvenioNacional)));
    assertThat(resultado.errores()).isEqualTo(1);
    assertThat(resultado.filas().getFirst().mensaje()).contains("es internacional");
  }

  @Test
  void detectaIdentidadesContradictorias() {
    var filaOriginal = fila(2, "IMP-Y", "Servicio contradicción", "3");
    var filaContradictoria = new Fila(
      3,
      filaOriginal.universidad(),
      filaOriginal.pais(),
      filaOriginal.programa(),
      "Otro nombre",
      filaOriginal.tipoDocumento(),
      filaOriginal.documento(),
      "",
      filaOriginal.servicio(),
      "3",
      "3",
      filaOriginal.asignatura(),
      filaOriginal.inicio(),
      filaOriginal.fin()
    );
    var resultado = carga.validar(request(List.of(filaOriginal, filaContradictoria)));
    assertThat(resultado.validos()).isEqualTo(1);
    assertThat(resultado.errores()).isEqualTo(1);
    assertThat(resultado.filas().get(1).mensaje()).contains("no coincide");
  }

  @Test
  void exigeRevalidarCuandoCambiaLaBase() {
    var solicitud = request(List.of(fila(2, "IMP-Z", "Servicio obsoleto", "1")));
    var validacionPrevia = carga.validar(solicitud);
    jdbc.update(
      "insert into universidad(nombre_universidad,pais) values('Cambio concurrente','CO')"
    );
    assertThatThrownBy(() ->
      carga.confirmar(new CargaInicialRequest(solicitud.filas(), true, validacionPrevia.validacion()))
    ).hasMessageContaining("Vuelve a validar");
    assertThat(
      jdbc.queryForObject(
        "select count(*) from residente where numero_documento='IMP-Z'",
        Integer.class
      )
    ).isZero();
  }

  @Test
  void fechasCuposYLongitudesSeValidanEnBackend() {
    for (String cupo : List.of("", "-1", "1.5", "2147483648"))
      assertThat(
        carga
          .validar(request(List.of(fila(2, "IMP-T", "Servicio tipos", cupo))))
          .errores()
      ).isEqualTo(1);
    assertThat(
      carga
        .validar(
          request(List.of(fila(2, "X".repeat(51), "Servicio longitud", "1")))
        )
        .errores()
    ).isEqualTo(1);
  }
}
