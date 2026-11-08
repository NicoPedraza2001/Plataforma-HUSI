package co.edu.javeriana.husi.casosdeuso;

import static co.edu.javeriana.husi.ClienteApiDePrueba.CONTRASENA_ADMINISTRADOR;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.ClienteApiDePrueba;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base de las pruebas que recorren la API real por HTTP, con cookie de sesión, token CSRF y filtros
 * de seguridad. Corren sobre H2 porque esas reglas no dependen del motor de base de datos.
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "spring.datasource.url=jdbc:h2:mem:casosdeusoapi;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;OPTIMIZE_REUSE_RESULTS=0;DB_CLOSE_DELAY=-1",
    "husi.seguridad.administrador-inicial.contrasena=" + CONTRASENA_ADMINISTRADOR,
  }
)
@ActiveProfiles("local")
abstract class BaseCasoDeUsoPorApi {

  static final String CONTRASENA_DE_CUENTA = "Cuenta-De-Prueba-1";
  static final int ID_ROL_ADMINISTRADOR = 1;
  static final int ID_ROL_COORDINADOR = 2;

  @LocalServerPort
  int puerto;

  @Autowired
  JdbcTemplate jdbc;

  /** Cuenta creada para una prueba; siempre tiene {@link #CONTRASENA_DE_CUENTA}. */
  record Cuenta(int id, String correo) {}

  ClienteApiDePrueba clienteSinSesion() {
    return new ClienteApiDePrueba(puerto);
  }

  ClienteApiDePrueba clienteAdministrador() throws Exception {
    return ClienteApiDePrueba.conSesionDeAdministrador(puerto);
  }

  ClienteApiDePrueba clienteDe(Cuenta cuenta) throws Exception {
    var cliente = clienteSinSesion();
    var inicio = cliente.iniciarSesion(cuenta.correo(), CONTRASENA_DE_CUENTA);
    assertThat(inicio.statusCode()).as(inicio.body()).isEqualTo(200);
    return cliente;
  }

  /** Crea un servicio con cupo para tres residentes simultáneos y devuelve su id. */
  int crearServicio() throws Exception {
    String nombre = "Servicio-" + UUID.randomUUID();
    var respuesta = clienteAdministrador().enviar(
      "POST",
      "servicios",
      "{\"nombre\":\"" + nombre + "\",\"cupoSimultaneo\":3,\"cupoMaximoInstalado\":3}"
    );
    assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(201);
    return jdbc.queryForObject(
      "select id_servicio from servicio where nombre=?",
      Integer.class,
      nombre
    );
  }

  Cuenta crearCoordinador() throws Exception {
    return crearCuenta(ID_ROL_COORDINADOR, crearServicio());
  }

  Cuenta crearAdministrador() throws Exception {
    return crearCuenta(ID_ROL_ADMINISTRADOR, null);
  }

  private Cuenta crearCuenta(int rolId, Integer servicioId) throws Exception {
    String correo = "cuenta-" + UUID.randomUUID() + "@example.test";
    var respuesta = clienteAdministrador().enviar(
      "POST",
      "usuarios",
      "{\"nombre\":\"Cuenta\",\"apellido\":\"Prueba\",\"correo\":\"" +
        correo +
        "\",\"rolId\":" +
        rolId +
        ",\"contrasena\":\"" +
        CONTRASENA_DE_CUENTA +
        "\"" +
        (servicioId == null ? "" : ",\"servicioId\":" + servicioId) +
        "}"
    );
    assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(201);
    int id = jdbc.queryForObject(
      "select id_usuario from usuario where correo=?",
      Integer.class,
      correo
    );
    return new Cuenta(id, correo);
  }
}
