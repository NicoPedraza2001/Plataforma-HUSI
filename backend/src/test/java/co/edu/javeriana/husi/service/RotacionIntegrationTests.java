package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.model.entity.RotacionId;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = { "spring.jpa.show-sql=false" })
@Transactional
// Los cambios se firman en el historial con el usuario en sesión.
@WithUserDetails("admin@husi.com")
class RotacionIntegrationTests {

  @Autowired
  RotacionService rotacionService;

  @Autowired
  ImportacionRotacionesService importacionRotacionesService;

  @Autowired
  JdbcTemplate jdbc;

  /** Crea un servicio con cupo 3 y una asignatura suya; devuelve el id de la asignatura. */
  private Integer asignatura() {
    Integer servicioId = jdbc.queryForObject(
      "insert into servicio(nombre,cupo_maximo_instalado,cupo_simultaneo) values(?,3,3) returning id_servicio",
      Integer.class,
      "TEST-ROT-S-" + UUID.randomUUID()
    );
    return jdbc.queryForObject(
      "insert into asignatura(id_servicio,nombre_asignatura) values(?,?) returning id_asignatura",
      Integer.class,
      servicioId,
      "TEST-ROT-A-" + UUID.randomUUID()
    );
  }

  private RotacionRequest datos(int asignaturaId) {
    return new RotacionRequest(
      asignaturaId,
      LocalDate.of(2091, 1, 1),
      LocalDate.of(2091, 1, 31)
    );
  }

  private RotacionId clave(RotacionRequest rotacion) {
    return new RotacionId(rotacion.asignaturaId(), rotacion.inicio(), rotacion.fin());
  }

  private Integer residente() {
    int universidadId = jdbc.queryForObject(
      "insert into universidad(nombre_universidad,pais) values(?,'Colombia') returning id_universidad",
      Integer.class,
      "TEST-ROT-U-" + UUID.randomUUID()
    );
    int programaId = jdbc.queryForObject(
      "insert into programa(id_universidad,nombre_programa) values(?,'TEST ROT') returning id_programa",
      Integer.class,
      universidadId
    );
    return jdbc.queryForObject(
      "insert into residente(id_programa,id_tipo_documento,nombre_completo,numero_documento) values(?,1,'Residente ficticio',?) returning id_residente",
      Integer.class,
      programaId,
      UUID.randomUUID().toString()
    );
  }

  @Test
  void crearEditarEstadoYEliminarPersistenSinPerderCupo() {
    var rotacion = datos(asignatura());
    var creada = rotacionService.crear(rotacion);
    assertThat(creada.cupoDisponible()).isEqualTo(3);
    assertThat(rotacionService.cambiarEstado(clave(rotacion), false).activa()).isEqualTo(false);
    assertThat(
      jdbc.queryForObject(
        "select count(*) from rotacion where id_asignatura=?",
        Integer.class,
        rotacion.asignaturaId()
      )
    ).isEqualTo(1);
    assertThat(rotacionService.cambiarEstado(clave(rotacion), true).activa()).isEqualTo(true);
    var editado = new RotacionRequest(
      rotacion.asignaturaId(),
      rotacion.inicio().plusDays(1),
      rotacion.fin()
    );
    assertThat(
      rotacionService.editar(clave(rotacion), editado).inicio().toString()
    ).isEqualTo("2091-01-02");
    assertThat(rotacionService.consultar(clave(editado)).cupoDisponible()).isEqualTo(
      3
    );
    rotacionService.eliminar(clave(editado));
    assertThat(
      jdbc.queryForObject(
        "select count(*) from rotacion where id_asignatura=?",
        Integer.class,
        rotacion.asignaturaId()
      )
    ).isZero();
  }

  @Test
  void protegeRelacionesInclusoCanceladas() {
    var rotacion = datos(asignatura());
    rotacionService.crear(rotacion);
    jdbc.update(
      "insert into rotacion_residente(id_residente,id_asignatura,fecha_inicio,fecha_fin,estado) values(?,?,?,?,'Cancelado')",
      residente(),
      rotacion.asignaturaId(),
      rotacion.inicio(),
      rotacion.fin()
    );
    assertThatThrownBy(() -> rotacionService.eliminar(clave(rotacion))).hasMessageContaining(
      "relacionados"
    );
    assertThatThrownBy(() ->
      rotacionService.editar(
        clave(rotacion),
        new RotacionRequest(rotacion.asignaturaId(), rotacion.inicio().plusDays(1), rotacion.fin())
      )
    ).hasMessageContaining("historial");
    assertThat(rotacionService.cambiarEstado(clave(rotacion), false).relaciones()).isEqualTo(1L);
  }

  @Test
  void inactivaBloqueaNuevasAsignacionesEnLaBase() {
    var rotacion = datos(asignatura());
    rotacionService.crear(rotacion);
    int residente = residente();
    rotacionService.cambiarEstado(clave(rotacion), false);
    assertThatThrownBy(() ->
      jdbc.update(
        "insert into rotacion_residente(id_residente,id_asignatura,fecha_inicio,fecha_fin) values(?,?,?,?)",
        residente,
        rotacion.asignaturaId(),
        rotacion.inicio(),
        rotacion.fin()
      )
    ).hasMessageContaining("inactiva");
  }

  @Test
  void cancelarAsignacionNoReactivaRotacion() {
    var rotacion = datos(asignatura());
    rotacionService.crear(rotacion);
    int residente = residente();
    jdbc.update(
      "insert into rotacion_residente(id_residente,id_asignatura,fecha_inicio,fecha_fin) values(?,?,?,?)",
      residente,
      rotacion.asignaturaId(),
      rotacion.inicio(),
      rotacion.fin()
    );
    rotacionService.cambiarEstado(clave(rotacion), false);
    jdbc.update(
      "update rotacion_residente set estado='Cancelado' where id_residente=?",
      residente
    );
    var consultada = rotacionService.consultar(clave(rotacion));
    assertThat(consultada.activa()).isEqualTo(false);
    assertThat(consultada.cupoDisponible()).isEqualTo(3);
  }

  @Test
  void busquedaFiltrosYPaginacionConsultanBase() {
    var rotacion = datos(asignatura());
    rotacionService.crear(rotacion);
    rotacionService.crear(
      new RotacionRequest(
        rotacion.asignaturaId(),
        rotacion.inicio().plusYears(1),
        rotacion.fin().plusYears(1)
      )
    );
    String nombre = rotacionService.consultar(clave(rotacion)).asignatura();
    var lista = rotacionService.listar(
      Map.of("q", nombre, "tamano", "1", "pagina", "2")
    );
    assertThat(lista.totalElementos()).isEqualTo(2);
    assertThat(lista.pagina()).isEqualTo(2);
    rotacionService.cambiarEstado(clave(rotacion), false);
    assertThat(
      rotacionService.listar(Map.of("q", nombre, "activa", "false")).totalElementos()
    ).isEqualTo(1);
    assertThat(
      rotacionService
        .listar(Map.of("q", "no-existe-" + UUID.randomUUID()))
        .totalElementos()
    ).isZero();
    assertThatThrownBy(() ->
      rotacionService.listar(Map.of("pagina", "0"))
    ).hasMessageContaining("válido");
  }

  @Test
  void filtraPorRangoDeFechasLasRotacionesQueSeCruzan() {
    var rotacion = datos(asignatura());
    rotacionService.crear(rotacion);
    var rotacionDelAnoSiguiente = new RotacionRequest(
      rotacion.asignaturaId(),
      rotacion.inicio().plusYears(1),
      rotacion.fin().plusYears(1)
    );
    rotacionService.crear(rotacionDelAnoSiguiente);
    String nombre = rotacionService.consultar(clave(rotacion)).asignatura();
    // Un rango que solo toca el último día de la primera rotación la incluye.
    assertThat(
      rotacionService
        .listar(Map.of("q", nombre, "desde", rotacion.fin().toString(), "hasta", rotacion.fin().toString()))
        .totalElementos()
    ).isEqualTo(1);
    assertThat(
      rotacionService
        .listar(Map.of("q", nombre, "desde", rotacion.inicio().toString()))
        .totalElementos()
    ).isEqualTo(2);
    assertThat(
      rotacionService
        .listar(Map.of("q", nombre, "hasta", rotacion.inicio().minusDays(1).toString()))
        .totalElementos()
    ).isZero();
    assertThatThrownBy(() ->
      rotacionService.listar(Map.of("desde", "2099-02-01", "hasta", "2099-01-01"))
    ).hasMessageContaining("no puede ser anterior");
    assertThatThrownBy(() ->
      rotacionService.listar(Map.of("desde", "31/01/2099"))
    ).hasMessageContaining("AAAA-MM-DD");
  }

  @Test
  void validaFechasDuplicadosYAsignatura() {
    var rotacion = datos(asignatura());
    rotacionService.crear(rotacion);
    assertThatThrownBy(() -> rotacionService.crear(rotacion)).hasMessageContaining(
      "ya existe"
    );
    assertThatThrownBy(() ->
      rotacionService.crear(new RotacionRequest(rotacion.asignaturaId(), rotacion.fin(), rotacion.inicio()))
    ).hasMessageContaining("anterior");
    assertThatThrownBy(() ->
      rotacionService.crear(datos(Integer.MAX_VALUE))
    ).hasMessageContaining("no existe");
    assertThatThrownBy(() ->
      rotacionService.crear(new RotacionRequest(null, null, null))
    ).hasMessageContaining("obligatorias");
  }

  @Test
  void importacionValidaSinGuardarOmiteDuplicadosYRegistraSoloValidas() {
    int asignaturaId = asignatura();
    String nombreAsignatura = jdbc.queryForObject(
      "select nombre_asignatura from asignatura where id_asignatura=?",
      String.class,
      asignaturaId
    );
    String nombreServicio = jdbc.queryForObject(
      "select s.nombre from servicio s join asignatura a using(id_servicio) where a.id_asignatura=?",
      String.class,
      asignaturaId
    );
    var solicitud = new ImportacionRotacionesRequest(
      List.of(
        new ImportacionRotacionesRequest.Fila(
          2,
          nombreAsignatura,
          nombreServicio,
          "2092-01-01",
          "2092-01-31"
        ),
        new ImportacionRotacionesRequest.Fila(
          3,
          nombreAsignatura,
          nombreServicio,
          "2092-01-01",
          "2092-01-31"
        ),
        new ImportacionRotacionesRequest.Fila(
          4,
          nombreAsignatura,
          nombreServicio,
          "mal",
          "2092-01-31"
        )
      )
    );
    var validacion = importacionRotacionesService.procesar(solicitud, false);
    assertThat(validacion.validos()).isEqualTo(1L);
    assertThat(validacion.omitidos()).isEqualTo(1L);
    assertThat(validacion.errores()).isEqualTo(1L);
    assertThat(
      jdbc.queryForObject(
        "select count(*) from rotacion where id_asignatura=?",
        Integer.class,
        asignaturaId
      )
    ).isZero();
    assertThat(importacionRotacionesService.procesar(solicitud, true).creados()).isEqualTo(1L);
    assertThat(importacionRotacionesService.procesar(solicitud, true).creados()).isEqualTo(0L);
  }

  @Autowired
  RotacionResidenteService rotacionResidenteService;

  @Test
  void asignaEsperaCancelaYReasignaSinAlterarHistorial() {
    var rotacion = datos(asignatura());
    rotacionService.crear(rotacion);
    var id = clave(rotacion);
    int uno = residente(),
      dos = residente(),
      tres = residente(),
      cuatro = residente();
    rotacionResidenteService.asignar(id, uno);
    rotacionResidenteService.asignar(id, dos);
    rotacionResidenteService.asignar(id, tres);
    rotacionResidenteService.asignar(id, cuatro);
    assertThat(rotacionService.consultar(id).cupoDisponible()).isZero();
    assertThat(rotacionResidenteService.listar(id, 1, true).contenido())
      .filteredOn(fila -> fila.residenteId().equals(cuatro))
      .extracting(
        co.edu.javeriana.husi.dto.response.ResidenteRotacionResponse::estado
      )
      .containsExactly("Esperando cupo");
    // El cupo liberado pasa solo al primero en espera.
    rotacionResidenteService.cambiarEstado(id, uno, "Cancelado");
    assertThat(rotacionService.consultar(id).cupoDisponible()).isZero();
    assertThat(rotacionResidenteService.listar(id, 1, false).contenido())
      .filteredOn(fila -> fila.residenteId().equals(cuatro))
      .extracting(
        co.edu.javeriana.husi.dto.response.ResidenteRotacionResponse::estado
      )
      .containsExactly("Asignado");
    assertThat(rotacionResidenteService.candidatos(id, ""))
      .extracting(co.edu.javeriana.husi.dto.response.CatalogoResponse::id)
      .doesNotContain(uno, dos, tres, cuatro);
    assertThatThrownBy(() ->
      rotacionResidenteService.cambiarEstado(id, uno, "Asignado")
    ).hasMessageContaining("cupos");
  }

  @Test
  void asignacionRechazaDuplicado() {
    var rotacion = datos(asignatura());
    rotacionService.crear(rotacion);
    int residenteId = residente();
    rotacionResidenteService.asignar(clave(rotacion), residenteId);
    assertThatThrownBy(() ->
      rotacionResidenteService.asignar(clave(rotacion), residenteId)
    ).hasMessageContaining("ya está");
  }

  @Test
  void inactivaPermiteCancelarPeroNoAsignar() {
    var rotacion = datos(asignatura());
    rotacionService.crear(rotacion);
    int residenteId = residente();
    rotacionResidenteService.asignar(clave(rotacion), residenteId);
    rotacionService.cambiarEstado(clave(rotacion), false);
    rotacionResidenteService.cambiarEstado(clave(rotacion), residenteId, "Cancelado");
    assertThat(rotacionService.consultar(clave(rotacion)).cupoDisponible()).isEqualTo(3);
    assertThatThrownBy(() ->
      rotacionResidenteService.asignar(clave(rotacion), residente())
    ).hasMessageContaining("activos");
  }

  @Test
  void liberarCupoUsaLaCapacidadVigenteDelServicio() {
    var rotacion = datos(asignatura());
    rotacionService.crear(rotacion);
    int residenteId = residente();
    rotacionResidenteService.asignar(clave(rotacion), residenteId);
    jdbc.update(
      "update servicio set cupo_simultaneo=1 where id_servicio=(select id_servicio from asignatura where id_asignatura=?)",
      rotacion.asignaturaId()
    );
    rotacionResidenteService.cambiarEstado(clave(rotacion), residenteId, "Cancelado");
    assertThat(rotacionService.consultar(clave(rotacion)).cupoDisponible()).isEqualTo(1);
  }

  private String estadoDe(RotacionRequest rotacion, int residenteId) {
    return jdbc.queryForObject(
      "select estado from rotacion_residente where id_residente=? and id_asignatura=? and fecha_inicio=? and fecha_fin=?",
      String.class,
      residenteId,
      rotacion.asignaturaId(),
      rotacion.inicio(),
      rotacion.fin()
    );
  }

  /** Rotación con cupo 3 llena y dos residentes en espera, en ese orden de llegada. */
  private List<Integer> rotacionLlenaConEspera(RotacionRequest rotacion) {
    rotacionService.crear(rotacion);
    var residentes = new ArrayList<Integer>();
    for (int i = 0; i < 5; i++) {
      residentes.add(residente());
      rotacionResidenteService.asignar(clave(rotacion), residentes.get(i));
    }
    assertThat(estadoDe(rotacion, residentes.get(3))).isEqualTo("Esperando cupo");
    assertThat(estadoDe(rotacion, residentes.get(4))).isEqualTo("Esperando cupo");
    return residentes;
  }

  @Test
  void cancelarAsignadoPromueveAlPrimeroEnEspera() {
    var rotacion = datos(asignatura());
    var residentes = rotacionLlenaConEspera(rotacion);
    rotacionResidenteService.cambiarEstado(clave(rotacion), residentes.get(0), "Cancelado");
    assertThat(estadoDe(rotacion, residentes.get(3))).isEqualTo("Asignado");
    assertThat(estadoDe(rotacion, residentes.get(4))).isEqualTo("Esperando cupo");
    assertThat(rotacionService.consultar(clave(rotacion)).cupoDisponible()).isZero();
  }

  @Test
  void ampliarCupoDelServicioPromueveLaEspera() {
    var rotacion = datos(asignatura());
    var residentes = rotacionLlenaConEspera(rotacion);
    jdbc.update(
      "update servicio set cupo_maximo_instalado=10,cupo_simultaneo=4 where id_servicio=(select id_servicio from asignatura where id_asignatura=?)",
      rotacion.asignaturaId()
    );
    assertThat(estadoDe(rotacion, residentes.get(3))).isEqualTo("Asignado");
    assertThat(estadoDe(rotacion, residentes.get(4))).isEqualTo("Esperando cupo");
    assertThat(rotacionService.consultar(clave(rotacion)).cupoDisponible()).isZero();
  }

  @Test
  void rotacionInactivaNoPromueveHastaReactivarse() {
    var rotacion = datos(asignatura());
    var residentes = rotacionLlenaConEspera(rotacion);
    rotacionService.cambiarEstado(clave(rotacion), false);
    rotacionResidenteService.cambiarEstado(clave(rotacion), residentes.get(0), "Cancelado");
    assertThat(estadoDe(rotacion, residentes.get(3))).isEqualTo("Esperando cupo");
    rotacionService.cambiarEstado(clave(rotacion), true);
    assertThat(estadoDe(rotacion, residentes.get(3))).isEqualTo("Asignado");
    assertThat(estadoDe(rotacion, residentes.get(4))).isEqualTo("Esperando cupo");
  }

  @Test
  void trasladaEsperaANuevaRotacionDelMismoServicio() {
    var origen = datos(asignatura());
    var residentes = rotacionLlenaConEspera(origen);
    Integer otraAsignaturaId = jdbc.queryForObject(
      "insert into asignatura(id_servicio,nombre_asignatura) values((select id_servicio from asignatura where id_asignatura=?),?) returning id_asignatura",
      Integer.class,
      origen.asignaturaId(),
      "TEST-ROT-A-" + UUID.randomUUID()
    );
    var destino = new RotacionRequest(
      otraAsignaturaId,
      LocalDate.of(2091, 3, 1),
      LocalDate.of(2091, 3, 31)
    );
    rotacionService.crear(destino);
    var espera = rotacionResidenteService.listarEsperaDelServicio(clave(destino));
    assertThat(espera)
      .extracting(enEspera -> enEspera.residenteId())
      .containsExactly(residentes.get(3), residentes.get(4));
    assertThat(espera).noneMatch(enEspera -> enEspera.cruce());
    var residenteEnEspera = new TrasladoEsperaRequest.Origen(
      residentes.get(3),
      origen.asignaturaId(),
      origen.inicio(),
      origen.fin()
    );
    rotacionResidenteService.trasladarEspera(
      clave(destino),
      new TrasladoEsperaRequest(List.of(residenteEnEspera))
    );
    assertThat(estadoDe(destino, residentes.get(3))).isEqualTo("Asignado");
    assertThat(rotacionService.consultar(clave(destino)).cupoDisponible()).isEqualTo(2);
    assertThat(rotacionResidenteService.listarEsperaDelServicio(clave(destino)))
      .extracting(enEspera -> enEspera.residenteId())
      .containsExactly(residentes.get(4));
    assertThatThrownBy(() ->
      rotacionResidenteService.trasladarEspera(
        clave(destino),
        new TrasladoEsperaRequest(List.of(residenteEnEspera))
      )
    ).hasMessageContaining("ya está relacionado");
  }

  @Test
  void trasladoNoSuperaLosCuposDelDestino() {
    var origen = datos(asignatura());
    var residentes = rotacionLlenaConEspera(origen);
    var destino = new RotacionRequest(
      origen.asignaturaId(),
      LocalDate.of(2091, 5, 1),
      LocalDate.of(2091, 5, 31)
    );
    rotacionService.crear(destino);
    rotacionResidenteService.asignar(clave(destino), residente());
    rotacionResidenteService.asignar(clave(destino), residente());
    var pedido = new TrasladoEsperaRequest(
      List.of(
        new TrasladoEsperaRequest.Origen(residentes.get(3), origen.asignaturaId(), origen.inicio(), origen.fin()),
        new TrasladoEsperaRequest.Origen(residentes.get(4), origen.asignaturaId(), origen.inicio(), origen.fin())
      )
    );
    assertThatThrownBy(() -> rotacionResidenteService.trasladarEspera(clave(destino), pedido))
      .hasMessageContaining("selecciona como máximo 1");
    assertThat(estadoDe(origen, residentes.get(3))).isEqualTo("Esperando cupo");
  }

  /** Inserta directamente (como la carga inicial) una rotación con fechas libres. */
  private RotacionRequest rotacionDirecta(LocalDate inicio, LocalDate fin) {
    var rotacion = new RotacionRequest(asignatura(), inicio, fin);
    jdbc.update(
      "insert into rotacion(id_asignatura,fecha_inicio,fecha_fin) values (?,?,?)",
      rotacion.asignaturaId(),
      inicio,
      fin
    );
    return rotacion;
  }

  @Test
  void noSeCreanNiMuevenRotacionesQueEmpiezanAntesDeHoy() {
    var hoy = co.edu.javeriana.husi.util.Calendario.hoy();
    var pasada = new RotacionRequest(asignatura(), hoy.minusDays(1), hoy.plusDays(10));
    assertThatThrownBy(() -> rotacionService.crear(pasada)).hasMessageContaining(
      "no puede ser anterior a hoy"
    );
    var deHoy = new RotacionRequest(asignatura(), hoy, hoy.plusDays(10));
    assertThat(rotacionService.crear(deHoy).finalizada()).isFalse();
    var movida = new RotacionRequest(deHoy.asignaturaId(), hoy.minusDays(3), hoy.plusDays(10));
    assertThatThrownBy(() -> rotacionService.editar(clave(deHoy), movida)).hasMessageContaining(
      "no puede ser anterior a hoy"
    );
  }

  @Test
  void rotacionFinalizadaEsDeSoloLectura() {
    var hoy = co.edu.javeriana.husi.util.Calendario.hoy();
    var rotacion = rotacionDirecta(hoy.minusDays(40), hoy.minusDays(1));
    var id = clave(rotacion);
    assertThat(rotacionService.consultar(id).finalizada()).isTrue();
    var otras = new RotacionRequest(rotacion.asignaturaId(), hoy.plusDays(1), hoy.plusDays(5));
    assertThatThrownBy(() -> rotacionService.editar(id, otras)).hasMessageContaining("finalizó");
    assertThatThrownBy(() -> rotacionService.cambiarEstado(id, false)).hasMessageContaining("finalizó");
    assertThatThrownBy(() -> rotacionService.eliminar(id)).hasMessageContaining("finalizó");
    assertThatThrownBy(() -> rotacionResidenteService.asignar(id, residente())).hasMessageContaining(
      "finalizó"
    );
    int historico = residente();
    rotacionResidenteService.asignarHistorica(id, historico);
    assertThatThrownBy(() -> rotacionResidenteService.cambiarEstado(id, historico, "Cancelado")).hasMessageContaining(
      "finalizó"
    );
    var origen = new TrasladoEsperaRequest.Origen(historico, rotacion.asignaturaId(), rotacion.inicio(), rotacion.fin());
    assertThatThrownBy(() ->
      rotacionResidenteService.trasladarEspera(id, new TrasladoEsperaRequest(List.of(origen)))
    ).hasMessageContaining("finalizó");
  }

  @Test
  void rotacionEnCursoAdmiteResidentesHastaSuFechaFinal() {
    var hoy = co.edu.javeriana.husi.util.Calendario.hoy();
    var rotacion = rotacionDirecta(hoy.minusDays(5), hoy);
    int residenteId = residente();
    rotacionResidenteService.asignar(clave(rotacion), residenteId);
    rotacionResidenteService.cambiarEstado(clave(rotacion), residenteId, "Cancelado");
    assertThat(rotacionService.consultar(clave(rotacion)).finalizada()).isFalse();
  }

  @Test
  void cambiarCupoDelServicioNoTocaRotacionesFinalizadas() {
    var hoy = co.edu.javeriana.husi.util.Calendario.hoy();
    var pasada = rotacionDirecta(hoy.minusDays(40), hoy.minusDays(1));
    for (int i = 0; i < 3; i++) rotacionResidenteService.asignarHistorica(clave(pasada), residente());
    var vigente = new RotacionRequest(pasada.asignaturaId(), hoy.plusDays(1), hoy.plusDays(9));
    rotacionService.crear(vigente);
    // Con 3 asignados en la finalizada, igual se puede bajar a 1: solo cuentan las vigentes.
    jdbc.update(
      "update servicio set cupo_simultaneo=1 where id_servicio=(select id_servicio from asignatura where id_asignatura=?)",
      pasada.asignaturaId()
    );
    assertThat(rotacionService.consultar(clave(pasada)).cupoDisponible()).isZero();
    assertThat(rotacionService.consultar(clave(vigente)).cupoDisponible()).isEqualTo(1);
  }

  @Test
  void listaLaEsperaAparteYPorOrdenDeLlegada() {
    var rotacion = datos(asignatura());
    var residentes = rotacionLlenaConEspera(rotacion);
    assertThat(rotacionResidenteService.listar(clave(rotacion), 1, true).contenido())
      .extracting(fila -> fila.residenteId())
      .containsExactly(residentes.get(3), residentes.get(4));
    var resto = rotacionResidenteService.listar(clave(rotacion), 1, false);
    assertThat(resto.totalElementos()).isEqualTo(3);
    assertThat(resto.contenido()).allMatch(fila -> fila.estado().equals("Asignado"));
  }

  @Test
  void quitarCupoMandaAlFinalDeLaEsperaYElCupoLoTomaElPrimero() {
    var rotacion = datos(asignatura());
    var residentes = rotacionLlenaConEspera(rotacion);
    rotacionResidenteService.cambiarEstado(clave(rotacion), residentes.get(0), "Esperando cupo");
    assertThat(estadoDe(rotacion, residentes.get(3))).isEqualTo("Asignado");
    assertThat(estadoDe(rotacion, residentes.get(0))).isEqualTo("Esperando cupo");
    assertThat(rotacionResidenteService.listar(clave(rotacion), 1, true).contenido())
      .extracting(fila -> fila.residenteId())
      .containsExactly(residentes.get(4), residentes.get(0));
    assertThat(rotacionService.consultar(clave(rotacion)).cupoDisponible()).isZero();
  }

  @Test
  void quitarCupoSinNadieEnEsperaDejaElCupoLibre() {
    var rotacion = datos(asignatura());
    rotacionService.crear(rotacion);
    int residenteId = residente();
    rotacionResidenteService.asignar(clave(rotacion), residenteId);
    rotacionResidenteService.cambiarEstado(clave(rotacion), residenteId, "Esperando cupo");
    assertThat(estadoDe(rotacion, residenteId)).isEqualTo("Esperando cupo");
    assertThat(rotacionService.consultar(clave(rotacion)).cupoDisponible()).isEqualTo(3);
  }

  @Test
  void soloSeEliminaUnaRelacionSinCupo() {
    var rotacion = datos(asignatura());
    var residentes = rotacionLlenaConEspera(rotacion);
    assertThatThrownBy(() -> rotacionResidenteService.eliminar(clave(rotacion), residentes.get(0)))
      .hasMessageContaining("Quítale el cupo");
    rotacionResidenteService.eliminar(clave(rotacion), residentes.get(3));
    assertThat(rotacionResidenteService.listar(clave(rotacion), 1, true).contenido())
      .extracting(fila -> fila.residenteId())
      .containsExactly(residentes.get(4));
    assertThat(rotacionService.consultar(clave(rotacion)).relaciones()).isEqualTo(4);
  }
}
