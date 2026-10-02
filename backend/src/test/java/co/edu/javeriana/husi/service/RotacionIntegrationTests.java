package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.model.entity.RotacionId;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = { "spring.jpa.show-sql=false" })
@Transactional
class RotacionIntegrationTests {

  @Autowired
  RotacionService servicio;

  @Autowired
  ImportacionRotacionesService importacion;

  @Autowired
  JdbcTemplate jdbc;

  private Integer asignatura() {
    Integer s = jdbc.queryForObject(
      "insert into servicio(nombre,cupo_maximo_instalado,cupo_simultaneo) values(?,3,3) returning id_servicio",
      Integer.class,
      "TEST-ROT-S-" + UUID.randomUUID()
    );
    return jdbc.queryForObject(
      "insert into asignatura(id_servicio,nombre_asignatura) values(?,?) returning id_asignatura",
      Integer.class,
      s,
      "TEST-ROT-A-" + UUID.randomUUID()
    );
  }

  private RotacionRequest datos(int id) {
    return new RotacionRequest(
      id,
      LocalDate.of(2091, 1, 1),
      LocalDate.of(2091, 1, 31)
    );
  }

  private RotacionId clave(RotacionRequest d) {
    return new RotacionId(d.asignaturaId(), d.inicio(), d.fin());
  }

  private Integer residente() {
    int u = jdbc.queryForObject(
      "insert into universidad(nombre_universidad,pais) values(?,'Colombia') returning id_universidad",
      Integer.class,
      "TEST-ROT-U-" + UUID.randomUUID()
    );
    int p = jdbc.queryForObject(
      "insert into programa(id_universidad,nombre_programa) values(?,'TEST ROT') returning id_programa",
      Integer.class,
      u
    );
    return jdbc.queryForObject(
      "insert into residente(id_programa,id_tipo_documento,nombre_completo,numero_documento) values(?,1,'Residente ficticio',?) returning id_residente",
      Integer.class,
      p,
      UUID.randomUUID().toString()
    );
  }

  @Test
  void crearEditarEstadoYEliminarPersistenSinPerderCupo() {
    var d = datos(asignatura());
    var r = servicio.crear(d);
    assertThat(r.cupoDisponible()).isEqualTo(3);
    assertThat(servicio.estado(clave(d), false).activa()).isEqualTo(false);
    assertThat(
      jdbc.queryForObject(
        "select count(*) from rotacion where id_asignatura=?",
        Integer.class,
        d.asignaturaId()
      )
    ).isEqualTo(1);
    assertThat(servicio.estado(clave(d), true).activa()).isEqualTo(true);
    var editado = new RotacionRequest(
      d.asignaturaId(),
      d.inicio().plusDays(1),
      d.fin()
    );
    assertThat(
      servicio.editar(clave(d), editado).inicio().toString()
    ).isEqualTo("2091-01-02");
    assertThat(servicio.consultar(clave(editado)).cupoDisponible()).isEqualTo(
      3
    );
    servicio.eliminar(clave(editado));
    assertThat(
      jdbc.queryForObject(
        "select count(*) from rotacion where id_asignatura=?",
        Integer.class,
        d.asignaturaId()
      )
    ).isZero();
  }

  @Test
  void protegeRelacionesInclusoCanceladas() {
    var d = datos(asignatura());
    servicio.crear(d);
    jdbc.update(
      "insert into rotacion_residente(id_residente,id_asignatura,fecha_inicio,fecha_fin,estado) values(?,?,?,?,'Cancelado')",
      residente(),
      d.asignaturaId(),
      d.inicio(),
      d.fin()
    );
    assertThatThrownBy(() -> servicio.eliminar(clave(d))).hasMessageContaining(
      "relacionados"
    );
    assertThatThrownBy(() ->
      servicio.editar(
        clave(d),
        new RotacionRequest(d.asignaturaId(), d.inicio().plusDays(1), d.fin())
      )
    ).hasMessageContaining("historial");
    assertThat(servicio.estado(clave(d), false).relaciones()).isEqualTo(1L);
  }

  @Test
  void inactivaBloqueaNuevasAsignacionesEnLaBase() {
    var d = datos(asignatura());
    servicio.crear(d);
    int residente = residente();
    servicio.estado(clave(d), false);
    assertThatThrownBy(() ->
      jdbc.update(
        "insert into rotacion_residente(id_residente,id_asignatura,fecha_inicio,fecha_fin) values(?,?,?,?)",
        residente,
        d.asignaturaId(),
        d.inicio(),
        d.fin()
      )
    ).hasMessageContaining("inactiva");
  }

  @Test
  void cancelarAsignacionNoReactivaRotacion() {
    var d = datos(asignatura());
    servicio.crear(d);
    int residente = residente();
    jdbc.update(
      "insert into rotacion_residente(id_residente,id_asignatura,fecha_inicio,fecha_fin) values(?,?,?,?)",
      residente,
      d.asignaturaId(),
      d.inicio(),
      d.fin()
    );
    servicio.estado(clave(d), false);
    jdbc.update(
      "update rotacion_residente set estado='Cancelado' where id_residente=?",
      residente
    );
    var r = servicio.consultar(clave(d));
    assertThat(r.activa()).isEqualTo(false);
    assertThat(r.cupoDisponible()).isEqualTo(3);
  }

  @Test
  void busquedaFiltrosYPaginacionConsultanBase() {
    var d = datos(asignatura());
    servicio.crear(d);
    servicio.crear(
      new RotacionRequest(
        d.asignaturaId(),
        d.inicio().plusYears(1),
        d.fin().plusYears(1)
      )
    );
    String nombre = servicio.consultar(clave(d)).asignatura();
    var lista = servicio.listar(
      Map.of("q", nombre, "tamano", "1", "pagina", "2")
    );
    assertThat(lista.totalElementos()).isEqualTo(2);
    assertThat(lista.pagina()).isEqualTo(2);
    servicio.estado(clave(d), false);
    assertThat(
      servicio.listar(Map.of("q", nombre, "activa", "false")).totalElementos()
    ).isEqualTo(1);
    assertThat(
      servicio
        .listar(Map.of("q", "no-existe-" + UUID.randomUUID()))
        .totalElementos()
    ).isZero();
    assertThatThrownBy(() ->
      servicio.listar(Map.of("pagina", "0"))
    ).hasMessageContaining("válido");
  }

  @Test
  void validaFechasDuplicadosYAsignatura() {
    var d = datos(asignatura());
    servicio.crear(d);
    assertThatThrownBy(() -> servicio.crear(d)).hasMessageContaining(
      "ya existe"
    );
    assertThatThrownBy(() ->
      servicio.crear(new RotacionRequest(d.asignaturaId(), d.fin(), d.inicio()))
    ).hasMessageContaining("anterior");
    assertThatThrownBy(() ->
      servicio.crear(datos(Integer.MAX_VALUE))
    ).hasMessageContaining("no existe");
    assertThatThrownBy(() ->
      servicio.crear(new RotacionRequest(null, null, null))
    ).hasMessageContaining("obligatorias");
  }

  @Test
  void importacionValidaSinGuardarOmiteDuplicadosYRegistraSoloValidas() {
    int id = asignatura();
    String a = jdbc.queryForObject(
      "select nombre_asignatura from asignatura where id_asignatura=?",
      String.class,
      id
    );
    String s = jdbc.queryForObject(
      "select s.nombre from servicio s join asignatura a using(id_servicio) where a.id_asignatura=?",
      String.class,
      id
    );
    var req = new ImportacionRotacionesRequest(
      List.of(
        new ImportacionRotacionesRequest.Fila(
          2,
          a,
          s,
          "2092-01-01",
          "2092-01-31"
        ),
        new ImportacionRotacionesRequest.Fila(
          3,
          a,
          s,
          "2092-01-01",
          "2092-01-31"
        ),
        new ImportacionRotacionesRequest.Fila(4, a, s, "mal", "2092-01-31")
      )
    );
    var validar = importacion.procesar(req, false);
    assertThat(validar.validos()).isEqualTo(1L);
    assertThat(validar.omitidos()).isEqualTo(1L);
    assertThat(validar.errores()).isEqualTo(1L);
    assertThat(
      jdbc.queryForObject(
        "select count(*) from rotacion where id_asignatura=?",
        Integer.class,
        id
      )
    ).isZero();
    assertThat(importacion.procesar(req, true).creados()).isEqualTo(1L);
    assertThat(importacion.procesar(req, true).creados()).isEqualTo(0L);
  }

  @Autowired
  RotacionResidenteService asignaciones;

  @Test
  void asignaEsperaCancelaYReasignaSinAlterarHistorial() {
    var d = datos(asignatura());
    servicio.crear(d);
    var id = clave(d);
    int uno = residente(),
      dos = residente(),
      tres = residente(),
      cuatro = residente();
    asignaciones.asignar(id, uno);
    asignaciones.asignar(id, dos);
    asignaciones.asignar(id, tres);
    asignaciones.asignar(id, cuatro);
    assertThat(servicio.consultar(id).cupoDisponible()).isZero();
    assertThat(asignaciones.listar(id, 1).contenido())
      .filteredOn(x -> x.residenteId().equals(cuatro))
      .extracting(
        co.edu.javeriana.husi.dto.response.ResidenteRotacionResponse::estado
      )
      .containsExactly("Esperando cupo");
    asignaciones.estado(id, uno, "Cancelado");
    assertThat(servicio.consultar(id).cupoDisponible()).isEqualTo(1);
    asignaciones.estado(id, cuatro, "Asignado");
    assertThat(servicio.consultar(id).cupoDisponible()).isZero();
    assertThat(asignaciones.candidatos(id, ""))
      .extracting(co.edu.javeriana.husi.dto.response.CatalogoResponse::id)
      .doesNotContain(uno, dos, tres, cuatro);
    assertThatThrownBy(() ->
      asignaciones.estado(id, uno, "Asignado")
    ).hasMessageContaining("cupos");
  }

  @Test
  void asignacionRechazaDuplicado() {
    var d = datos(asignatura());
    servicio.crear(d);
    int r = residente();
    asignaciones.asignar(clave(d), r);
    assertThatThrownBy(() ->
      asignaciones.asignar(clave(d), r)
    ).hasMessageContaining("ya está");
  }

  @Test
  void inactivaPermiteCancelarPeroNoAsignar() {
    var d = datos(asignatura());
    servicio.crear(d);
    int r = residente();
    asignaciones.asignar(clave(d), r);
    servicio.estado(clave(d), false);
    asignaciones.estado(clave(d), r, "Cancelado");
    assertThat(servicio.consultar(clave(d)).cupoDisponible()).isEqualTo(3);
    assertThatThrownBy(() ->
      asignaciones.asignar(clave(d), residente())
    ).hasMessageContaining("activos");
  }

  @Test
  void liberarCupoRespetaCapacidadCongeladaAunqueServicioCambie() {
    var d = datos(asignatura());
    servicio.crear(d);
    int r = residente();
    asignaciones.asignar(clave(d), r);
    jdbc.update(
      "update servicio set cupo_simultaneo=1 where id_servicio=(select id_servicio from asignatura where id_asignatura=?)",
      d.asignaturaId()
    );
    asignaciones.estado(clave(d), r, "Cancelado");
    assertThat(servicio.consultar(clave(d)).cupoDisponible()).isEqualTo(3);
  }
}
