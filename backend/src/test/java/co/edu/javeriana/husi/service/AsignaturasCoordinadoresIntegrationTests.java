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

  private Integer idRol(String rol) {
    return jdbc.queryForObject(
      "select id_rol from rol where lower(nombre_rol)=lower(?)",
      Integer.class,
      rol
    );
  }

  /** Datos de un usuario de prueba; el servicio solo cuenta si el rol es Coordinador. */
  private UsuarioRequest datosUsuario(String correo, String rol, Integer servicioId) {
    return new UsuarioRequest("Prueba", "Coordinación", correo, idRol(rol), "PruebaAC2026!", servicioId);
  }

  private UsuarioResponse coordinador(Integer servicioId) {
    return usuarios.crear(
      datosUsuario(UUID.randomUUID() + "@example.test", "Coordinador", servicioId)
    );
  }

  private Integer servicioCoordinado(Integer usuarioId) {
    return jdbc
      .queryForList(
        "select id_servicio from coordinador where id_usuario=?",
        Integer.class,
        usuarioId
      )
      .stream()
      .findFirst()
      .orElse(null);
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
  void elCoordinadorSeCreaDesdeUsuariosConSuServicio() {
    var servicioCreado = servicio();
    var otro = servicio();
    var creado = coordinador(servicioCreado.id());
    assertThat(creado.servicioId()).isEqualTo(servicioCreado.id());
    assertThat(servicioCoordinado(creado.id())).isEqualTo(servicioCreado.id());

    var editado = usuarios.actualizar(
      creado.id(),
      datosUsuario(creado.correo(), "Coordinador", otro.id())
    );
    assertThat(editado.servicio()).isEqualTo(otro.nombre());
    assertThat(
      consultas.listar("usuarios", Map.of("q", otro.nombre())).totalElementos()
    ).isEqualTo(1);

    // El estado del usuario se copia a su coordinación (el dashboard la cuenta).
    usuarios.cambiarEstado(creado.id(), "Inactivo");
    assertThat(
      jdbc.queryForObject(
        "select estado from coordinador where id_usuario=?",
        String.class,
        creado.id()
      )
    ).isEqualTo("Inactivo");
  }

  @Test
  void elCoordinadorExigeUnServicioActivo() {
    String correo = UUID.randomUUID() + "@example.test";
    assertThatThrownBy(() -> usuarios.crear(datosUsuario(correo, "Coordinador", null)))
      .hasMessageContaining("Selecciona el servicio");
    var inactivo = servicio();
    servicios.cambiarEstado(inactivo.id(), "Inactivo");
    assertThatThrownBy(() -> usuarios.crear(datosUsuario(correo, "Coordinador", inactivo.id())))
      .hasMessageContaining("servicio activo");
  }

  @Test
  void conservaElServicioInactivoQueYaCoordinaba() {
    var servicioCreado = servicio();
    var creado = coordinador(servicioCreado.id());
    servicios.cambiarEstado(servicioCreado.id(), "Inactivo");
    assertThat(
      usuarios
        .actualizar(creado.id(), datosUsuario(creado.correo(), "Coordinador", servicioCreado.id()))
        .servicioId()
    ).isEqualTo(servicioCreado.id());
  }

  @Test
  void variosCoordinadoresPuedenCompartirServicio() {
    var servicioCreado = servicio();
    coordinador(servicioCreado.id());
    coordinador(servicioCreado.id());
    assertThat(
      jdbc.queryForObject(
        "select count(*) from coordinador where id_servicio=?",
        Integer.class,
        servicioCreado.id()
      )
    ).isEqualTo(2);
  }

  @Test
  void elAdministradorNoGuardaServicio() {
    var servicioCreado = servicio();
    var admin = usuarios.crear(
      datosUsuario(UUID.randomUUID() + "@example.test", "Administrador", servicioCreado.id())
    );
    assertThat(admin.servicioId()).isNull();
    assertThat(servicioCoordinado(admin.id())).isNull();
  }

  @Test
  void alDejarDeSerCoordinadorSeRetiraSuServicio() {
    var creado = coordinador(servicio().id());
    var ahoraAdmin = usuarios.actualizar(
      creado.id(),
      datosUsuario(creado.correo(), "Administrador", null)
    );
    assertThat(ahoraAdmin.servicioId()).isNull();
    assertThat(servicioCoordinado(creado.id())).isNull();
  }

  @Test
  void eliminarElUsuarioRetiraSuCoordinacionPeroNoElServicio() {
    var servicioCreado = servicio();
    var creado = coordinador(servicioCreado.id());
    usuarios.eliminarDefinitivamente(creado.id());
    assertThat(servicioCoordinado(creado.id())).isNull();
    assertThat(servicios.consultar(servicioCreado.id()).id()).isEqualTo(servicioCreado.id());
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
  void filtrosInvalidosSeRechazan() {
    assertThatThrownBy(() ->
      consultas.listar("asignaturas", Map.of("servicioId", "x"))
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() ->
      consultas.listar("usuarios", Map.of("inventado", "x"))
    ).isInstanceOf(NegocioException.class);
  }
}
