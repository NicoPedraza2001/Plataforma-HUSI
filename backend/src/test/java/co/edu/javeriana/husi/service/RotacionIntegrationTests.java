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
    assertThat(asignaciones.listar(id, 1, true).contenido())
      .filteredOn(x -> x.residenteId().equals(cuatro))
      .extracting(
        co.edu.javeriana.husi.dto.response.ResidenteRotacionResponse::estado
      )
      .containsExactly("Esperando cupo");
    // El cupo liberado pasa solo al primero en espera.
    asignaciones.estado(id, uno, "Cancelado");
    assertThat(servicio.consultar(id).cupoDisponible()).isZero();
    assertThat(asignaciones.listar(id, 1, false).contenido())
      .filteredOn(x -> x.residenteId().equals(cuatro))
      .extracting(
        co.edu.javeriana.husi.dto.response.ResidenteRotacionResponse::estado
      )
      .containsExactly("Asignado");
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
  void liberarCupoUsaLaCapacidadVigenteDelServicio() {
    var d = datos(asignatura());
    servicio.crear(d);
    int r = residente();
    asignaciones.asignar(clave(d), r);
    jdbc.update(
      "update servicio set cupo_simultaneo=1 where id_servicio=(select id_servicio from asignatura where id_asignatura=?)",
      d.asignaturaId()
    );
    asignaciones.estado(clave(d), r, "Cancelado");
    assertThat(servicio.consultar(clave(d)).cupoDisponible()).isEqualTo(1);
  }

  private String estadoDe(RotacionRequest d, int residente) {
    return jdbc.queryForObject(
      "select estado from rotacion_residente where id_residente=? and id_asignatura=? and fecha_inicio=? and fecha_fin=?",
      String.class,
      residente,
      d.asignaturaId(),
      d.inicio(),
      d.fin()
    );
  }

  /** Rotación con cupo 3 llena y dos residentes en espera, en ese orden de llegada. */
  private List<Integer> rotacionLlenaConEspera(RotacionRequest d) {
    servicio.crear(d);
    var r = new ArrayList<Integer>();
    for (int i = 0; i < 5; i++) {
      r.add(residente());
      asignaciones.asignar(clave(d), r.get(i));
    }
    assertThat(estadoDe(d, r.get(3))).isEqualTo("Esperando cupo");
    assertThat(estadoDe(d, r.get(4))).isEqualTo("Esperando cupo");
    return r;
  }

  @Test
  void cancelarAsignadoPromueveAlPrimeroEnEspera() {
    var d = datos(asignatura());
    var r = rotacionLlenaConEspera(d);
    asignaciones.estado(clave(d), r.get(0), "Cancelado");
    assertThat(estadoDe(d, r.get(3))).isEqualTo("Asignado");
    assertThat(estadoDe(d, r.get(4))).isEqualTo("Esperando cupo");
    assertThat(servicio.consultar(clave(d)).cupoDisponible()).isZero();
  }

  @Test
  void ampliarCupoDelServicioPromueveLaEspera() {
    var d = datos(asignatura());
    var r = rotacionLlenaConEspera(d);
    jdbc.update(
      "update servicio set cupo_maximo_instalado=10,cupo_simultaneo=4 where id_servicio=(select id_servicio from asignatura where id_asignatura=?)",
      d.asignaturaId()
    );
    assertThat(estadoDe(d, r.get(3))).isEqualTo("Asignado");
    assertThat(estadoDe(d, r.get(4))).isEqualTo("Esperando cupo");
    assertThat(servicio.consultar(clave(d)).cupoDisponible()).isZero();
  }

  @Test
  void rotacionInactivaNoPromueveHastaReactivarse() {
    var d = datos(asignatura());
    var r = rotacionLlenaConEspera(d);
    servicio.estado(clave(d), false);
    asignaciones.estado(clave(d), r.get(0), "Cancelado");
    assertThat(estadoDe(d, r.get(3))).isEqualTo("Esperando cupo");
    servicio.estado(clave(d), true);
    assertThat(estadoDe(d, r.get(3))).isEqualTo("Asignado");
    assertThat(estadoDe(d, r.get(4))).isEqualTo("Esperando cupo");
  }

  @Test
  void trasladaEsperaANuevaRotacionDelMismoServicio() {
    var origen = datos(asignatura());
    var r = rotacionLlenaConEspera(origen);
    Integer otra = jdbc.queryForObject(
      "insert into asignatura(id_servicio,nombre_asignatura) values((select id_servicio from asignatura where id_asignatura=?),?) returning id_asignatura",
      Integer.class,
      origen.asignaturaId(),
      "TEST-ROT-A-" + UUID.randomUUID()
    );
    var destino = new RotacionRequest(
      otra,
      LocalDate.of(2091, 3, 1),
      LocalDate.of(2091, 3, 31)
    );
    servicio.crear(destino);
    var espera = asignaciones.espera(clave(destino));
    assertThat(espera)
      .extracting(e -> e.residenteId())
      .containsExactly(r.get(3), r.get(4));
    assertThat(espera).noneMatch(e -> e.cruce());
    var o = new TrasladoEsperaRequest.Origen(
      r.get(3),
      origen.asignaturaId(),
      origen.inicio(),
      origen.fin()
    );
    asignaciones.trasladarEspera(
      clave(destino),
      new TrasladoEsperaRequest(List.of(o))
    );
    assertThat(estadoDe(destino, r.get(3))).isEqualTo("Asignado");
    assertThat(servicio.consultar(clave(destino)).cupoDisponible()).isEqualTo(2);
    assertThat(asignaciones.espera(clave(destino)))
      .extracting(e -> e.residenteId())
      .containsExactly(r.get(4));
    assertThatThrownBy(() ->
      asignaciones.trasladarEspera(
        clave(destino),
        new TrasladoEsperaRequest(List.of(o))
      )
    ).hasMessageContaining("ya está relacionado");
  }

  @Test
  void trasladoNoSuperaLosCuposDelDestino() {
    var origen = datos(asignatura());
    var r = rotacionLlenaConEspera(origen);
    var destino = new RotacionRequest(
      origen.asignaturaId(),
      LocalDate.of(2091, 5, 1),
      LocalDate.of(2091, 5, 31)
    );
    servicio.crear(destino);
    asignaciones.asignar(clave(destino), residente());
    asignaciones.asignar(clave(destino), residente());
    var pedido = new TrasladoEsperaRequest(
      List.of(
        new TrasladoEsperaRequest.Origen(r.get(3), origen.asignaturaId(), origen.inicio(), origen.fin()),
        new TrasladoEsperaRequest.Origen(r.get(4), origen.asignaturaId(), origen.inicio(), origen.fin())
      )
    );
    assertThatThrownBy(() -> asignaciones.trasladarEspera(clave(destino), pedido))
      .hasMessageContaining("selecciona como máximo 1");
    assertThat(estadoDe(origen, r.get(3))).isEqualTo("Esperando cupo");
  }

  /** Inserta directamente (como la carga inicial) una rotación con fechas libres. */
  private RotacionRequest rotacionDirecta(LocalDate inicio, LocalDate fin) {
    var d = new RotacionRequest(asignatura(), inicio, fin);
    jdbc.update(
      "insert into rotacion(id_asignatura,fecha_inicio,fecha_fin) values (?,?,?)",
      d.asignaturaId(),
      inicio,
      fin
    );
    return d;
  }

  @Test
  void noSeCreanNiMuevenRotacionesQueEmpiezanAntesDeHoy() {
    var hoy = co.edu.javeriana.husi.util.Calendario.hoy();
    var pasada = new RotacionRequest(asignatura(), hoy.minusDays(1), hoy.plusDays(10));
    assertThatThrownBy(() -> servicio.crear(pasada)).hasMessageContaining(
      "no puede ser anterior a hoy"
    );
    var deHoy = new RotacionRequest(asignatura(), hoy, hoy.plusDays(10));
    assertThat(servicio.crear(deHoy).finalizada()).isFalse();
    var movida = new RotacionRequest(deHoy.asignaturaId(), hoy.minusDays(3), hoy.plusDays(10));
    assertThatThrownBy(() -> servicio.editar(clave(deHoy), movida)).hasMessageContaining(
      "no puede ser anterior a hoy"
    );
  }

  @Test
  void rotacionFinalizadaEsDeSoloLectura() {
    var hoy = co.edu.javeriana.husi.util.Calendario.hoy();
    var d = rotacionDirecta(hoy.minusDays(40), hoy.minusDays(1));
    var id = clave(d);
    assertThat(servicio.consultar(id).finalizada()).isTrue();
    var otras = new RotacionRequest(d.asignaturaId(), hoy.plusDays(1), hoy.plusDays(5));
    assertThatThrownBy(() -> servicio.editar(id, otras)).hasMessageContaining("finalizó");
    assertThatThrownBy(() -> servicio.estado(id, false)).hasMessageContaining("finalizó");
    assertThatThrownBy(() -> servicio.eliminar(id)).hasMessageContaining("finalizó");
    assertThatThrownBy(() -> asignaciones.asignar(id, residente())).hasMessageContaining(
      "finalizó"
    );
    int historico = residente();
    asignaciones.asignarHistorica(id, historico);
    assertThatThrownBy(() -> asignaciones.estado(id, historico, "Cancelado")).hasMessageContaining(
      "finalizó"
    );
    var origen = new TrasladoEsperaRequest.Origen(historico, d.asignaturaId(), d.inicio(), d.fin());
    assertThatThrownBy(() ->
      asignaciones.trasladarEspera(id, new TrasladoEsperaRequest(List.of(origen)))
    ).hasMessageContaining("finalizó");
  }

  @Test
  void rotacionEnCursoAdmiteResidentesHastaSuFechaFinal() {
    var hoy = co.edu.javeriana.husi.util.Calendario.hoy();
    var d = rotacionDirecta(hoy.minusDays(5), hoy);
    int r = residente();
    asignaciones.asignar(clave(d), r);
    asignaciones.estado(clave(d), r, "Cancelado");
    assertThat(servicio.consultar(clave(d)).finalizada()).isFalse();
  }

  @Test
  void cambiarCupoDelServicioNoTocaRotacionesFinalizadas() {
    var hoy = co.edu.javeriana.husi.util.Calendario.hoy();
    var pasada = rotacionDirecta(hoy.minusDays(40), hoy.minusDays(1));
    for (int i = 0; i < 3; i++) asignaciones.asignarHistorica(clave(pasada), residente());
    var vigente = new RotacionRequest(pasada.asignaturaId(), hoy.plusDays(1), hoy.plusDays(9));
    servicio.crear(vigente);
    // Con 3 asignados en la finalizada, igual se puede bajar a 1: solo cuentan las vigentes.
    jdbc.update(
      "update servicio set cupo_simultaneo=1 where id_servicio=(select id_servicio from asignatura where id_asignatura=?)",
      pasada.asignaturaId()
    );
    assertThat(servicio.consultar(clave(pasada)).cupoDisponible()).isZero();
    assertThat(servicio.consultar(clave(vigente)).cupoDisponible()).isEqualTo(1);
  }

  @Test
  void listaLaEsperaAparteYPorOrdenDeLlegada() {
    var d = datos(asignatura());
    var r = rotacionLlenaConEspera(d);
    assertThat(asignaciones.listar(clave(d), 1, true).contenido())
      .extracting(x -> x.residenteId())
      .containsExactly(r.get(3), r.get(4));
    var resto = asignaciones.listar(clave(d), 1, false);
    assertThat(resto.totalElementos()).isEqualTo(3);
    assertThat(resto.contenido()).allMatch(x -> x.estado().equals("Asignado"));
  }

  @Test
  void quitarCupoMandaAlFinalDeLaEsperaYElCupoLoTomaElPrimero() {
    var d = datos(asignatura());
    var r = rotacionLlenaConEspera(d);
    asignaciones.estado(clave(d), r.get(0), "Esperando cupo");
    assertThat(estadoDe(d, r.get(3))).isEqualTo("Asignado");
    assertThat(estadoDe(d, r.get(0))).isEqualTo("Esperando cupo");
    assertThat(asignaciones.listar(clave(d), 1, true).contenido())
      .extracting(x -> x.residenteId())
      .containsExactly(r.get(4), r.get(0));
    assertThat(servicio.consultar(clave(d)).cupoDisponible()).isZero();
  }

  @Test
  void quitarCupoSinNadieEnEsperaDejaElCupoLibre() {
    var d = datos(asignatura());
    servicio.crear(d);
    int r = residente();
    asignaciones.asignar(clave(d), r);
    asignaciones.estado(clave(d), r, "Esperando cupo");
    assertThat(estadoDe(d, r)).isEqualTo("Esperando cupo");
    assertThat(servicio.consultar(clave(d)).cupoDisponible()).isEqualTo(3);
  }

  @Test
  void soloSeEliminaUnaRelacionSinCupo() {
    var d = datos(asignatura());
    var r = rotacionLlenaConEspera(d);
    assertThatThrownBy(() -> asignaciones.eliminar(clave(d), r.get(0)))
      .hasMessageContaining("Quítale el cupo");
    asignaciones.eliminar(clave(d), r.get(3));
    assertThat(asignaciones.listar(clave(d), 1, true).contenido())
      .extracting(x -> x.residenteId())
      .containsExactly(r.get(4));
    assertThat(servicio.consultar(clave(d)).relaciones()).isEqualTo(4);
  }
}
