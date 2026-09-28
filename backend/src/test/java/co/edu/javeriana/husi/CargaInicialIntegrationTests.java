package co.edu.javeriana.husi;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.CargaInicialRequest;
import co.edu.javeriana.husi.dto.request.CargaInicialRequest.Fila;
import co.edu.javeriana.husi.service.CargaInicialService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CargaInicialIntegrationTests {

  @Autowired
  CargaInicialService carga;

  @Autowired
  JdbcTemplate jdbc;

  private Fila fila(int n, String doc, String servicio, String cupos) {
    return new Fila(
      n,
      "Universidad importación",
      "Colombia",
      "Programa importación",
      "Residente " + doc,
      "CC",
      doc,
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
  void respetaPrioridadSinCrearHuerfanosDeFilasSinCupo() {
    var d = request(
      List.of(
        fila(3, "IMP-B", "Servicio prioridad", "1"),
        fila(2, "IMP-A", "Servicio prioridad", "1"),
        fila(4, "IMP-B", "Servicio prioridad", "1")
      )
    );
    var r = carga.validar(d);
    assertThat(r.validos()).isEqualTo(1);
    assertThat(r.advertencias()).isEqualTo(1);
    assertThat(r.omitidos()).isEqualTo(1);
    assertThat(r.filas().getFirst().fila()).isEqualTo(3);
    carga.confirmar(new CargaInicialRequest(d.filas(), true, r.validacion()));
    assertThat(
      jdbc.queryForObject(
        "select count(*) from residente where numero_documento='IMP-A'",
        Integer.class
      )
    ).isZero();
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
  void detectaIdentidadesContradictorias() {
    var a = fila(2, "IMP-Y", "Servicio contradicción", "3");
    var b = new Fila(
      3,
      a.universidad(),
      a.pais(),
      a.programa(),
      "Otro nombre",
      a.tipoDocumento(),
      a.documento(),
      "",
      a.servicio(),
      "3",
      "3",
      a.asignatura(),
      a.inicio(),
      a.fin()
    );
    var r = carga.validar(request(List.of(a, b)));
    assertThat(r.validos()).isEqualTo(1);
    assertThat(r.errores()).isEqualTo(1);
    assertThat(r.filas().get(1).mensaje()).contains("no coincide");
  }

  @Test
  void exigeRevalidarCuandoCambiaLaBase() {
    var d = request(List.of(fila(2, "IMP-Z", "Servicio obsoleto", "1")));
    var p = carga.validar(d);
    jdbc.update(
      "insert into universidad(nombre_universidad,pais) values('Cambio concurrente','CO')"
    );
    assertThatThrownBy(() ->
      carga.confirmar(new CargaInicialRequest(d.filas(), true, p.validacion()))
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
