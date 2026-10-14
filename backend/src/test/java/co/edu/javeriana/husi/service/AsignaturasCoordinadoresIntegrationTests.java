package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.dto.response.*;
import co.edu.javeriana.husi.exception.NegocioException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
// Los cambios se firman en el historial con el usuario en sesión.
@WithUserDetails("admin@husi.com")
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
    var servicioCreado = servicio();
    var asignaturaCreada = asignaturas.crear(new AsignaturaRequest("Materia uno", servicioCreado.id()));
    asignaturas.crear(new AsignaturaRequest("Materia dos", servicioCreado.id()));
    assertThat(
      jdbc.queryForObject(
        "select id_servicio from asignatura where id_asignatura=?",
        Integer.class,
        asignaturaCreada.id()
      )
    ).isEqualTo(servicioCreado.id());
    var pagina = consultas.listar(
      "asignaturas",
      Map.of("servicioId", servicioCreado.id().toString(), "tamano", "1", "pagina", "2")
    );
    assertThat(pagina.totalElementos()).isEqualTo(2);
    assertThat(pagina.pagina()).isEqualTo(2);
    assertThat(pagina.contenido()).hasSize(1);
    asignaturas.actualizar(
      asignaturaCreada.id(),
      new AsignaturaRequest("Materia editada", servicioCreado.id())
    );
    asignaturas.cambiarEstado(asignaturaCreada.id(), "Inactivo");
    assertThat(
      consultas
        .listar(
          "asignaturas",
          Map.of(
            "q",
            "editada",
            "servicioId",
            servicioCreado.id().toString(),
            "estado",
            "Inactivo"
          )
        )
        .totalElementos()
    ).isEqualTo(1);
    assertThat(asignaturas.cambiarEstado(asignaturaCreada.id(), "Activo").activo()).isTrue();
    asignaturas.eliminarDefinitivamente(asignaturaCreada.id());
    assertThat(
      jdbc.queryForObject(
        "select count(*) from asignatura where id_asignatura=?",
        Integer.class,
        asignaturaCreada.id()
      )
    ).isZero();
  }

  @Test
  void asignaturaDuplicadaSeRechazaPorServicio() {
    var servicioCreado = servicio();
    asignaturas.crear(new AsignaturaRequest("Materia", servicioCreado.id()));
    assertThatThrownBy(() ->
      asignaturas.crear(new AsignaturaRequest(" materia ", servicioCreado.id()))
    ).isInstanceOf(NegocioException.class);
  }

  @Test
  void asignaturaPuedeRepetirseEnOtroServicio() {
    var servicioCreado = servicio();
    var otro = servicio();
    asignaturas.crear(new AsignaturaRequest("Materia", servicioCreado.id()));
    assertThat(
      asignaturas
        .crear(new AsignaturaRequest("Materia", otro.id()))
        .servicioId()
    ).isEqualTo(otro.id());
  }

  @Test
  void rotacionesProtegenAsignatura() {
    var servicioCreado = servicio();
    var otro = servicio();
    var asignaturaCreada = asignaturas.crear(new AsignaturaRequest("Materia", servicioCreado.id()));
    jdbc.update(
      "insert into rotacion(id_asignatura,fecha_inicio,fecha_fin,cupo_disponible) values (?,'2099-01-01','2099-01-02',2)",
      asignaturaCreada.id()
    );
    assertThatThrownBy(() ->
      asignaturas.eliminarDefinitivamente(asignaturaCreada.id())
    ).hasMessageContaining("rotaciones");
  }

  @Test
  void rotacionesBloqueanCambioDeServicio() {
    var servicioCreado = servicio();
    var otro = servicio();
    var asignaturaCreada = asignaturas.crear(new AsignaturaRequest("Materia", servicioCreado.id()));
    jdbc.update(
      "insert into rotacion(id_asignatura,fecha_inicio,fecha_fin,cupo_disponible) values (?,'2099-01-01','2099-01-02',2)",
      asignaturaCreada.id()
    );
    assertThatThrownBy(() ->
      asignaturas.actualizar(
        asignaturaCreada.id(),
        new AsignaturaRequest("Materia", otro.id())
      )
    ).hasMessageContaining("historial");
  }

  @Test
  void coordinadorCompletaFlujoYNoBorraUsuario() {
    var servicioCreado = servicio();
    var otro = servicio();
    var usuarioCreado = usuario("Coordinador");
    assertThat(coordinadores.usuariosDisponibles())
      .extracting(CatalogoResponse::id)
      .contains(usuarioCreado.id());
    var coordinadorCreado = coordinadores.crear(new CoordinadorRequest(usuarioCreado.id(), servicioCreado.id()));
    assertThat(coordinadores.usuariosDisponibles())
      .extracting(CatalogoResponse::id)
      .doesNotContain(usuarioCreado.id());
    coordinadores.actualizar(coordinadorCreado.id(), new CoordinadorRequest(usuarioCreado.id(), otro.id()));
    coordinadores.cambiarEstado(coordinadorCreado.id(), "Inactivo");
    assertThat(
      consultas
        .listar(
          "coordinadores",
          Map.of(
            "q",
            usuarioCreado.correo(),
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
        coordinadorCreado.id()
      )
    ).isEqualTo(otro.id());
    assertThat(coordinadores.cambiarEstado(coordinadorCreado.id(), "Activo").activo()).isTrue();
    coordinadores.eliminarDefinitivamente(coordinadorCreado.id());
    assertThat(usuarios.consultar(usuarioCreado.id()).id()).isEqualTo(usuarioCreado.id());
    assertThat(servicios.consultar(otro.id()).id()).isEqualTo(otro.id());
    assertThat(coordinadores.usuariosDisponibles())
      .extracting(CatalogoResponse::id)
      .contains(usuarioCreado.id());
  }

  @Test
  void noDuplicaUsuarioAunqueCoordinadorInactivo() {
    var servicioCreado = servicio();
    var usuarioCreado = usuario("Coordinador");
    var coordinadorCreado = coordinadores.crear(new CoordinadorRequest(usuarioCreado.id(), servicioCreado.id()));
    coordinadores.cambiarEstado(coordinadorCreado.id(), "Inactivo");
    assertThatThrownBy(() ->
      coordinadores.crear(new CoordinadorRequest(usuarioCreado.id(), servicioCreado.id()))
    ).hasMessageContaining("ya tiene");
  }

  @Test
  void rechazaRolDistinto() {
    var servicioCreado = servicio();
    var usuarioCreado = usuario("Administrador");
    assertThatThrownBy(() ->
      coordinadores.crear(new CoordinadorRequest(usuarioCreado.id(), servicioCreado.id()))
    ).hasMessageContaining("rol Coordinador");
  }

  @Test
  void rechazaUsuarioInactivo() {
    var servicioCreado = servicio();
    var usuarioCreado = usuario("Coordinador");
    usuarios.cambiarEstado(usuarioCreado.id(), "Inactivo");
    assertThatThrownBy(() ->
      coordinadores.crear(new CoordinadorRequest(usuarioCreado.id(), servicioCreado.id()))
    ).hasMessageContaining("activo");
  }

  @Test
  void rechazaServicioInactivo() {
    var servicioCreado = servicio();
    servicios.cambiarEstado(servicioCreado.id(), "Inactivo");
    assertThat(servicios.catalogo(true))
      .extracting(CatalogoResponse::id)
      .doesNotContain(servicioCreado.id());
    assertThat(servicios.catalogo(false))
      .extracting(CatalogoResponse::id)
      .contains(servicioCreado.id());
    assertThatThrownBy(() ->
      asignaturas.crear(new AsignaturaRequest("Materia", servicioCreado.id()))
    ).hasMessageContaining("activo");
  }

  @Test
  void conservaReferenciaActualInactivaAlEditar() {
    var servicioCreado = servicio();
    var asignaturaCreada = asignaturas.crear(new AsignaturaRequest("Materia", servicioCreado.id()));
    servicios.cambiarEstado(servicioCreado.id(), "Inactivo");
    assertThat(
      asignaturas
        .actualizar(asignaturaCreada.id(), new AsignaturaRequest("Editada", servicioCreado.id()))
        .nombre()
    ).isEqualTo("Editada");
  }

  @Test
  void noCambiaRolMientrasTieneCoordinacion() {
    var servicioCreado = servicio();
    var usuarioCreado = usuario("Coordinador");
    coordinadores.crear(new CoordinadorRequest(usuarioCreado.id(), servicioCreado.id()));
    Integer rol = jdbc.queryForObject(
      "select id_rol from rol where nombre_rol='Administrador'",
      Integer.class
    );
    assertThatThrownBy(() ->
      usuarios.actualizar(
        usuarioCreado.id(),
        new UsuarioRequest("Prueba", "Coordinación", usuarioCreado.correo(), rol, "")
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
