package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.exception.NegocioException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PersonasIntegrationTests {

  @Autowired
  ResidenteService residentes;

  @Autowired
  UsuarioService usuarios;

  @Autowired
  ConsultaService consultas;

  @Autowired
  JdbcTemplate jdbc;

  private Integer programa() {
    return jdbc.queryForObject(
      "select p.id_programa from programa p join universidad u using(id_universidad) where p.estado='Activo' and u.estado='Activo' order by p.id_programa limit 1",
      Integer.class
    );
  }

  private Integer rol() {
    return jdbc.queryForObject("select min(id_rol) from rol", Integer.class);
  }

  private String clave = "PruebaCRUD2026!";

  private UsuarioRequest usuario(String correo, String password) {
    return new UsuarioRequest("Prueba", "Integración", correo, rol(), password);
  }

  @Test
  void residentePersisteEditaFiltraCambiaEstadoYElimina() {
    String doc = UUID.randomUUID().toString();
    var d = new ResidenteRequest(
      "Prueba CRUD",
      "CC",
      doc,
      "Posgrado",
      programa()
    );
    var e = residentes.crear(d);
    assertThat(
      jdbc.queryForObject(
        "select numero_documento from residente where id_residente=?",
        String.class,
        e.id()
      )
    ).isEqualTo(doc);
    var editado = residentes.actualizar(
      e.id(),
      new ResidenteRequest(
        "Editado CRUD",
        "CE",
        doc,
        "Especialidad",
        programa()
      )
    );
    assertThat(editado.tipoDocumento()).isEqualTo("CE");
    residentes.cambiarEstado(e.id(), "Inactivo");
    assertThat(
      consultas
        .listar("residentes", Map.of("q", doc, "estado", "Inactivo"))
        .totalElementos()
    ).isEqualTo(1);
    residentes.cambiarEstado(e.id(), "Activo");
    residentes.eliminarDefinitivamente(e.id());
    assertThat(
      jdbc.queryForObject(
        "select count(*) from residente where id_residente=?",
        Integer.class,
        e.id()
      )
    ).isZero();
  }

  @Test
  void residenteRechazaDuplicadosYProgramaInvalido() {
    String doc = UUID.randomUUID().toString();
    var d = new ResidenteRequest("Prueba", "CC", doc, null, programa());
    residentes.crear(d);
    assertThatThrownBy(() -> residentes.crear(d)).isInstanceOf(
      NegocioException.class
    );
    assertThatThrownBy(() ->
      residentes.crear(
        new ResidenteRequest("Prueba", "CC", "otro", null, Integer.MAX_VALUE)
      )
    ).isInstanceOf(NegocioException.class);
  }

  @Test
  void residenteAsociadoNoSeBorra() {
    Integer id = jdbc.queryForObject(
      "select id_residente from rotacion_residente limit 1",
      Integer.class
    );
    assertThatThrownBy(() -> residentes.eliminarDefinitivamente(id))
      .isInstanceOf(NegocioException.class)
      .hasMessageContaining("asociadas");
  }

  @Test
  void usuarioPersisteHashEditaSinCambiarClaveYElimina() {
    String correo = "crud-" + UUID.randomUUID() + "@example.test";
    var e = usuarios.crear(usuario(correo, clave));
    String hash = jdbc.queryForObject(
      "select contrasena from usuario where id_usuario=?",
      String.class,
      e.id()
    );
    assertThat(new BCryptPasswordEncoder().matches(clave, hash)).isTrue();
    usuarios.actualizar(
      e.id(),
      new UsuarioRequest("Editado", "Apellido", correo, rol(), "")
    );
    assertThat(
      jdbc.queryForObject(
        "select contrasena from usuario where id_usuario=?",
        String.class,
        e.id()
      )
    ).isEqualTo(hash);
    usuarios.cambiarEstado(e.id(), "Inactivo");
    assertThat(
      consultas
        .listar("usuarios", Map.of("q", correo, "estado", "Inactivo"))
        .totalElementos()
    ).isEqualTo(1);
    usuarios.actualizar(e.id(), usuario(correo, "NuevaClave2026!"));
    assertThat(
      new BCryptPasswordEncoder().matches(
        "NuevaClave2026!",
        jdbc.queryForObject(
          "select contrasena from usuario where id_usuario=?",
          String.class,
          e.id()
        )
      )
    ).isTrue();
    usuarios.eliminarDefinitivamente(e.id());
    assertThat(
      jdbc.queryForObject(
        "select count(*) from usuario where id_usuario=?",
        Integer.class,
        e.id()
      )
    ).isZero();
  }

  @Test
  void usuarioRechazaCorreoDuplicadoClaveCortaYRolInvalido() {
    String correo = "crud-" + UUID.randomUUID() + "@example.test";
    usuarios.crear(usuario(correo, clave));
    assertThatThrownBy(() ->
      usuarios.crear(usuario(correo.toUpperCase(), clave))
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() ->
      usuarios.crear(usuario("otro@example.test", "123"))
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() ->
      usuarios.crear(
        new UsuarioRequest(
          "A",
          "B",
          "otro@example.test",
          Integer.MAX_VALUE,
          clave
        )
      )
    ).isInstanceOf(NegocioException.class);
  }

  @Test
  void usuarioConAuditoriaNoSeBorra() {
    var e = usuarios.crear(
      usuario("crud-" + UUID.randomUUID() + "@example.test", clave)
    );
    jdbc.update(
      "insert into historial_auditoria(id_usuario,accion_realizada,entidad_modificada) values (?, 'Prueba','Usuario')",
      e.id()
    );
    assertThatThrownBy(() -> usuarios.eliminarDefinitivamente(e.id()))
      .isInstanceOf(NegocioException.class)
      .hasMessageContaining("auditoría");
  }
}
