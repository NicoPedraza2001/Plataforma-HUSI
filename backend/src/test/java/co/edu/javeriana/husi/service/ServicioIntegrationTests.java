package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.ServicioRequest;
import co.edu.javeriana.husi.exception.NegocioException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ServicioIntegrationTests {

  @Autowired
  ServicioService servicios;

  @Autowired
  ConsultaService consultas;

  @Autowired
  JdbcTemplate jdbc;

  private ServicioRequest datos(String nombre, int simultaneo, int maximo) {
    return new ServicioRequest(
      nombre,
      "Descripción de prueba",
      simultaneo,
      maximo
    );
  }

  @Test
  void crearEditarBuscarCambiarEstadoYEliminar() {
    String nombre = "SERVICIO-" + UUID.randomUUID();
    var s = servicios.crear(datos(nombre, 2, 5));
    assertThat(
      jdbc.queryForObject(
        "select nombre from servicio where id_servicio=?",
        String.class,
        s.id()
      )
    ).isEqualTo(nombre);
    servicios.cambiarEstado(s.id(), "Inactivo");
    var editado = servicios.actualizar(
      s.id(),
      datos(nombre + " editado", 3, 6)
    );
    assertThat(editado.activo()).isFalse();
    assertThat(editado.cupoSimultaneo()).isEqualTo(3);
    assertThat(
      consultas
        .listar("servicios", Map.of("q", nombre, "estado", "Inactivo"))
        .totalElementos()
    ).isEqualTo(1);
    assertThat(servicios.cambiarEstado(s.id(), "Activo").activo()).isTrue();
    servicios.eliminarDefinitivamente(s.id());
    assertThat(
      jdbc.queryForObject(
        "select count(*) from servicio where id_servicio=?",
        Integer.class,
        s.id()
      )
    ).isZero();
    assertThatThrownBy(() -> servicios.consultar(s.id())).isInstanceOf(
      NegocioException.class
    );
  }

  @Test
  void validaNombreDuplicadoYCapacidades() {
    String nombre = "SERVICIO-" + UUID.randomUUID();
    servicios.crear(datos(nombre, 0, 0));
    assertThatThrownBy(() ->
      servicios.crear(datos(nombre.toLowerCase(), 0, 0))
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() -> servicios.crear(datos(" ", 0, 0))).isInstanceOf(
      NegocioException.class
    );
    assertThatThrownBy(() ->
      servicios.crear(datos("Otro", -1, 2))
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() -> servicios.crear(datos("Otro", 3, 2))).isInstanceOf(
      NegocioException.class
    );
  }

  @Test
  void asignaturasInclusoInactivasBloqueanBorrado() {
    var s = servicios.crear(datos("SERVICIO-" + UUID.randomUUID(), 2, 5));
    jdbc.update(
      "insert into asignatura(id_servicio,nombre_asignatura,estado) values (?, 'Prueba','Inactivo')",
      s.id()
    );
    assertThatThrownBy(() -> servicios.eliminarDefinitivamente(s.id()))
      .isInstanceOf(NegocioException.class)
      .hasMessageContaining("asignaturas");
    servicios.cambiarEstado(s.id(), "Inactivo");
    assertThat(servicios.consultar(s.id()).activo()).isFalse();
  }

  @Test
  void coordinadoresBloqueanBorrado() {
    var s = servicios.crear(datos("SERVICIO-" + UUID.randomUUID(), 2, 5));
    Integer u = jdbc.queryForObject(
      "insert into usuario(id_rol,nombre,apellido,correo,contrasena) values ((select min(id_rol) from rol),'Prueba','Servicio',?, 'hash-test-no-login') returning id_usuario",
      Integer.class,
      UUID.randomUUID() + "@example.test"
    );
    jdbc.update(
      "insert into coordinador(id_usuario,id_servicio) values (?,?)",
      u,
      s.id()
    );
    assertThatThrownBy(() -> servicios.eliminarDefinitivamente(s.id()))
      .isInstanceOf(NegocioException.class)
      .hasMessageContaining("coordinadores");
  }

  @Test
  void cambiarCupoNoModificaRotacionesExistentes() {
    var s = servicios.crear(datos("SERVICIO-" + UUID.randomUUID(), 2, 5));
    Integer a = jdbc.queryForObject(
      "insert into asignatura(id_servicio,nombre_asignatura) values (?,'Prueba') returning id_asignatura",
      Integer.class,
      s.id()
    );
    jdbc.update(
      "insert into rotacion(id_asignatura,fecha_inicio,fecha_fin,cupo_disponible) values (?,'2099-01-01','2099-01-02',2)",
      a
    );
    servicios.actualizar(s.id(), datos(s.nombre(), 4, 5));
    assertThat(
      jdbc.queryForObject(
        "select cupo_disponible from rotacion where id_asignatura=?",
        Integer.class,
        a
      )
    ).isEqualTo(2);
  }
  @Autowired tools.jackson.databind.json.JsonMapper json;
  @Test void jsonRechazaCuposDecimales() {
    assertThatThrownBy(() -> json.readValue("{\"nombre\":\"Prueba\",\"cupoSimultaneo\":1.5,\"cupoMaximoInstalado\":5}", ServicioRequest.class)).isInstanceOf(tools.jackson.core.JacksonException.class);
  }}
