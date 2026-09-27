package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AsignaturasCoordinadoresIntegrationTests {

  @Autowired
  AsignaturaService asignaturas;

  @Autowired
  CoordinadorService coordinadores;

  @Autowired
  ServicioService servicios;

  @Autowired
  UsuarioService usuarios;

  @Autowired
  ConsultaService consultas;

  @Autowired
  JdbcTemplate jdbc;

  private ServicioResponse servicio() {
    return servicios.crear(
      new ServicioRequest("AC-" + UUID.randomUUID(), "Integración", 2, 5)
    );
  }

  private UsuarioResponse usuario(String rol) {
    Integer id = jdbc.queryForObject(
      "select id_rol from rol where lower(nombre_rol)=lower(?)",
      Integer.class,
      rol
    );
    return usuarios.crear(
      new UsuarioRequest(
        "Prueba",
        "Coordinación",
        UUID.randomUUID() + "@example.test",
        id,
        "PruebaAC2026!"
      )
    );
  }

  @Test
  void asignaturaPersisteEditaBuscaPaginaCambiaEstadoYElimina() {
    var s = servicio();
    var a = asignaturas.crear(new AsignaturaRequest("Materia uno", s.id()));
    asignaturas.crear(new AsignaturaRequest("Materia dos", s.id()));
    assertThat(
      jdbc.queryForObject(
        "select id_servicio from asignatura where id_asignatura=?",
        Integer.class,
        a.id()
      )
    ).isEqualTo(s.id());
    var pagina = consultas.listar(
      "asignaturas",
      Map.of("servicioId", s.id().toString(), "tamano", "1", "pagina", "2")
    );
    assertThat(pagina.totalElementos()).isEqualTo(2);
    assertThat(pagina.pagina()).isEqualTo(2);
    assertThat(pagina.contenido()).hasSize(1);
    asignaturas.actualizar(
      a.id(),
      new AsignaturaRequest("Materia editada", s.id())
    );
    asignaturas.cambiarEstado(a.id(), "Inactivo");
    assertThat(
      consultas
        .listar(
          "asignaturas",
          Map.of(
            "q",
            "editada",
            "servicioId",
            s.id().toString(),
            "estado",
            "Inactivo"
          )
        )
        .totalElementos()
    ).isEqualTo(1);
    assertThat(asignaturas.cambiarEstado(a.id(), "Activo").activo()).isTrue();
    asignaturas.eliminarDefinitivamente(a.id());
    assertThat(
      jdbc.queryForObject(
        "select count(*) from asignatura where id_asignatura=?",
        Integer.class,
        a.id()
      )
    ).isZero();
  }

  @Test
  void asignaturaDuplicadaSeRechazaPorServicio() {
    var s = servicio();
    asignaturas.crear(new AsignaturaRequest("Materia", s.id()));
    assertThatThrownBy(() ->
      asignaturas.crear(new AsignaturaRequest(" materia ", s.id()))
    ).isInstanceOf(NegocioException.class);
  }

  @Test
  void asignaturaPuedeRepetirseEnOtroServicio() {
    var s = servicio();
    var otro = servicio();
    asignaturas.crear(new AsignaturaRequest("Materia", s.id()));
    assertThat(
      asignaturas
        .crear(new AsignaturaRequest("Materia", otro.id()))
        .servicioId()
    ).isEqualTo(otro.id());
  }

  @Test
  void rotacionesProtegenAsignatura() {
    var s = servicio();
    var otro = servicio();
    var a = asignaturas.crear(new AsignaturaRequest("Materia", s.id()));
    jdbc.update(
      "insert into rotacion(id_asignatura,fecha_inicio,fecha_fin,cupo_disponible) values (?,'2099-01-01','2099-01-02',2)",
      a.id()
    );
    assertThatThrownBy(() ->
      asignaturas.eliminarDefinitivamente(a.id())
    ).hasMessageContaining("rotaciones");
  }

  @Test
  void rotacionesBloqueanCambioDeServicio() {
    var s = servicio();
    var otro = servicio();
    var a = asignaturas.crear(new AsignaturaRequest("Materia", s.id()));
    jdbc.update(
      "insert into rotacion(id_asignatura,fecha_inicio,fecha_fin,cupo_disponible) values (?,'2099-01-01','2099-01-02',2)",
      a.id()
    );
    assertThatThrownBy(() ->
      asignaturas.actualizar(
        a.id(),
        new AsignaturaRequest("Materia", otro.id())
      )
    ).hasMessageContaining("historial");
  }

  @Test
  void coordinadorCompletaFlujoYNoBorraUsuario() {
    var s = servicio();
    var otro = servicio();
    var u = usuario("Coordinador");
    assertThat(coordinadores.usuariosDisponibles())
      .extracting(CatalogoResponse::id)
      .contains(u.id());
    var c = coordinadores.crear(new CoordinadorRequest(u.id(), s.id()));
    assertThat(coordinadores.usuariosDisponibles())
      .extracting(CatalogoResponse::id)
      .doesNotContain(u.id());
    coordinadores.actualizar(c.id(), new CoordinadorRequest(u.id(), otro.id()));
    coordinadores.cambiarEstado(c.id(), "Inactivo");
    assertThat(
      consultas
        .listar(
          "coordinadores",
          Map.of(
            "q",
            u.correo(),
            "estado",
            "Inactivo",
            "servicioId",
            otro.id().toString()
          )
        )
        .totalElementos()
    ).isEqualTo(1);
    assertThat(
      jdbc.queryForObject(
        "select id_servicio from coordinador where id_coordinador=?",
        Integer.class,
        c.id()
      )
    ).isEqualTo(otro.id());
    assertThat(coordinadores.cambiarEstado(c.id(), "Activo").activo()).isTrue();
    coordinadores.eliminarDefinitivamente(c.id());
    assertThat(usuarios.consultar(u.id()).id()).isEqualTo(u.id());
    assertThat(servicios.consultar(otro.id()).id()).isEqualTo(otro.id());
    assertThat(coordinadores.usuariosDisponibles())
      .extracting(CatalogoResponse::id)
      .contains(u.id());
  }

  @Test
  void noDuplicaUsuarioAunqueCoordinadorInactivo() {
    var s = servicio();
    var u = usuario("Coordinador");
    var c = coordinadores.crear(new CoordinadorRequest(u.id(), s.id()));
    coordinadores.cambiarEstado(c.id(), "Inactivo");
    assertThatThrownBy(() ->
      coordinadores.crear(new CoordinadorRequest(u.id(), s.id()))
    ).hasMessageContaining("ya tiene");
  }

  @Test
  void rechazaRolDistinto() {
    var s = servicio();
    var u = usuario("Administrador");
    assertThatThrownBy(() ->
      coordinadores.crear(new CoordinadorRequest(u.id(), s.id()))
    ).hasMessageContaining("rol Coordinador");
  }

  @Test
  void rechazaUsuarioInactivo() {
    var s = servicio();
    var u = usuario("Coordinador");
    usuarios.cambiarEstado(u.id(), "Inactivo");
    assertThatThrownBy(() ->
      coordinadores.crear(new CoordinadorRequest(u.id(), s.id()))
    ).hasMessageContaining("activo");
  }

  @Test
  void rechazaServicioInactivo() {
    var s = servicio();
    servicios.cambiarEstado(s.id(), "Inactivo");
    assertThat(servicios.catalogo(true))
      .extracting(CatalogoResponse::id)
      .doesNotContain(s.id());
    assertThat(servicios.catalogo(false))
      .extracting(CatalogoResponse::id)
      .contains(s.id());
    assertThatThrownBy(() ->
      asignaturas.crear(new AsignaturaRequest("Materia", s.id()))
    ).hasMessageContaining("activo");
  }

  @Test
  void conservaReferenciaActualInactivaAlEditar() {
    var s = servicio();
    var a = asignaturas.crear(new AsignaturaRequest("Materia", s.id()));
    servicios.cambiarEstado(s.id(), "Inactivo");
    assertThat(
      asignaturas
        .actualizar(a.id(), new AsignaturaRequest("Editada", s.id()))
        .nombre()
    ).isEqualTo("Editada");
  }

  @Test
  void noCambiaRolMientrasTieneCoordinacion() {
    var s = servicio();
    var u = usuario("Coordinador");
    coordinadores.crear(new CoordinadorRequest(u.id(), s.id()));
    Integer rol = jdbc.queryForObject(
      "select id_rol from rol where nombre_rol='Administrador'",
      Integer.class
    );
    assertThatThrownBy(() ->
      usuarios.actualizar(
        u.id(),
        new UsuarioRequest("Prueba", "Coordinación", u.correo(), rol, "")
      )
    ).hasMessageContaining("asignación");
  }

  @Test
  void filtrosInvalidosSeRechazan() {
    assertThatThrownBy(() ->
      consultas.listar("asignaturas", Map.of("servicioId", "x"))
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() ->
      consultas.listar("coordinadores", Map.of("inventado", "x"))
    ).isInstanceOf(NegocioException.class);
  }
}
