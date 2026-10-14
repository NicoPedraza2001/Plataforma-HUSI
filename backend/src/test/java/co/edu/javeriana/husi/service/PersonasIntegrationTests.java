package co.edu.javeriana.husi.service;

import static org.assertj.core.api.Assertions.*;

import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.exception.NegocioException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
// Los cambios se firman en el historial con el usuario en sesión.
@WithUserDetails("admin@husi.com")
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
    return new UsuarioRequest("Prueba", "Integración", correo, rol(), password, null);
  }

  @Test
  void buscaResidentesPorDocumentosSinDistinguirMayusculas() {
    String documento = "Doc-" + UUID.randomUUID();
    var creado = residentes.crear(
      new ResidenteRequest("Prueba Lote", "CC", documento, "", programa())
    );
    var encontrados = residentes.buscarPorDocumentos(
      List.of(" " + documento.toLowerCase() + " ", "no-existe-" + UUID.randomUUID())
    );
    assertThat(encontrados).hasSize(1);
    assertThat(encontrados.getFirst().id()).isEqualTo(creado.id());
    assertThat(encontrados.getFirst().tipoDocumento()).isEqualTo("CC");
    assertThat(residentes.buscarPorDocumentos(List.of())).isEmpty();
  }

  @Test
  void residentePersisteEditaFiltraCambiaEstadoYElimina() {
    String documento = UUID.randomUUID().toString();
    var solicitud = new ResidenteRequest(
      "Prueba CRUD",
      "CC",
      documento,
      "Posgrado",
      programa()
    );
    var registroCreado = residentes.crear(solicitud);
    assertThat(
      jdbc.queryForObject(
        "select numero_documento from residente where id_residente=?",
        String.class,
        registroCreado.id()
      )
    ).isEqualTo(documento);
    var editado = residentes.actualizar(
      registroCreado.id(),
      new ResidenteRequest(
        "Editado CRUD",
        "CE",
        documento,
        "Pregrado",
        programa()
      )
    );
    assertThat(editado.tipoDocumento()).isEqualTo("CE");
    assertThat(editado.perfilAcademico()).isEqualTo("Pregrado");
    residentes.cambiarEstado(registroCreado.id(), "Inactivo");
    assertThat(
      consultas
        .listar("residentes", Map.of("q", documento, "estado", "Inactivo"))
        .totalElementos()
    ).isEqualTo(1);
    residentes.cambiarEstado(registroCreado.id(), "Activo");
    residentes.eliminarDefinitivamente(registroCreado.id());
    assertThat(
      jdbc.queryForObject(
        "select count(*) from residente where id_residente=?",
        Integer.class,
        registroCreado.id()
      )
    ).isZero();
  }

  @Test
  void residenteRechazaDuplicadosYProgramaInvalido() {
    String documento = UUID.randomUUID().toString();
    var solicitud = new ResidenteRequest("Prueba", "CC", documento, null, programa());
    residentes.crear(solicitud);
    assertThatThrownBy(() -> residentes.crear(solicitud)).isInstanceOf(
      NegocioException.class
    );
    assertThatThrownBy(() ->
      residentes.crear(
        new ResidenteRequest("Prueba", "CC", "otro", null, Integer.MAX_VALUE)
      )
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() ->
      residentes.crear(
        new ResidenteRequest("Prueba", "XX", "otro", null, programa())
      )
    ).isInstanceOf(NegocioException.class);
    assertThatThrownBy(() ->
      residentes.crear(
        new ResidenteRequest("Prueba", "CC", "otro", "Doctorado", programa())
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
    var registroCreado = usuarios.crear(usuario(correo, clave));
    String hash = jdbc.queryForObject(
      "select contrasena from usuario where id_usuario=?",
      String.class,
      registroCreado.id()
    );
    assertThat(new BCryptPasswordEncoder().matches(clave, hash)).isTrue();
    usuarios.actualizar(
      registroCreado.id(),
      new UsuarioRequest("Editado", "Apellido", correo, rol(), "", null)
    );
    assertThat(
      jdbc.queryForObject(
        "select contrasena from usuario where id_usuario=?",
        String.class,
        registroCreado.id()
      )
    ).isEqualTo(hash);
    usuarios.cambiarEstado(registroCreado.id(), "Inactivo");
    assertThat(
      consultas
        .listar("usuarios", Map.of("q", correo, "estado", "Inactivo"))
        .totalElementos()
    ).isEqualTo(1);
    usuarios.actualizar(registroCreado.id(), usuario(correo, "NuevaClave2026!"));
    assertThat(
      new BCryptPasswordEncoder().matches(
        "NuevaClave2026!",
        jdbc.queryForObject(
          "select contrasena from usuario where id_usuario=?",
          String.class,
          registroCreado.id()
        )
      )
    ).isTrue();
    usuarios.eliminarDefinitivamente(registroCreado.id());
    assertThat(
      jdbc.queryForObject(
        "select count(*) from usuario where id_usuario=?",
        Integer.class,
        registroCreado.id()
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
          clave,
          null
        )
      )
    ).isInstanceOf(NegocioException.class);
  }

  @Test
  void usuarioConAuditoriaNoSeBorra() {
    var registroCreado = usuarios.crear(
      usuario("crud-" + UUID.randomUUID() + "@example.test", clave)
    );
    jdbc.update(
      "insert into historial_auditoria(id_usuario,accion_realizada,entidad_modificada) values (?, 'Prueba','Usuario')",
      registroCreado.id()
    );
    assertThatThrownBy(() -> usuarios.eliminarDefinitivamente(registroCreado.id()))
      .isInstanceOf(NegocioException.class)
      .hasMessageContaining("auditoría");
  }
}
