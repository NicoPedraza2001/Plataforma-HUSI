package co.edu.javeriana.husi;

import static org.assertj.core.api.Assertions.*;

import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = "husi.seguridad.administrador-inicial.contrasena=" +
  ClienteApiDePrueba.CONTRASENA_ADMINISTRADOR
)
@ActiveProfiles("local")
class LocalH2IntegrationTests {

  @LocalServerPort
  int port;

  private ClienteApiDePrueba administrador;

  /** Todas las llamadas de esta clase se hacen con la sesión del administrador. */
  private ClienteApiDePrueba administrador() throws Exception {
    if (administrador == null) administrador = ClienteApiDePrueba.conSesionDeAdministrador(port);
    return administrador;
  }

  private HttpResponse<String> get(String ruta) throws Exception {
    return administrador().get(ruta);
  }

  @Test
  void todosLosListadosFuncionanSinPostgres() throws Exception {
    for (String ruta : new String[] {
      "universidades",
      "programas",
      "residentes",
      "usuarios",
      "servicios",
      "asignaturas",
      "coordinadores",
      "rotaciones",
      "rotaciones/gestion",
      "rotaciones/gestion/catalogos",
      "historial",
      "dashboard",
      "dashboard/relaciones",
      "asignaturas/catalogo",
      "catalogos",
    }) {
      var respuesta = get(ruta);
      assertThat(respuesta.statusCode())
        .as(ruta + ": " + respuesta.body())
        .isEqualTo(200);
    }
  }

  private HttpResponse<String> enviar(String metodo, String ruta, String cuerpo)
    throws Exception {
    return administrador().enviar(metodo, ruta, cuerpo);
  }

  private int crear(String ruta, String cuerpo) throws Exception {
    var respuesta = enviar("POST", ruta, cuerpo);
    assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(201);
    return tools.jackson.databind.json.JsonMapper.builder()
      .build()
      .readTree(respuesta.body())
      .get("id")
      .asInt();
  }

  @Test
  void crudRelacionadosYCuposEnH2() throws Exception {
    int uni = crear(
      "universidades",
      "{\"nombre\":\"Universidad local test\",\"pais\":\"Colombia\"}"
    );
    int prog = crear(
      "programas",
      "{\"nombre\":\"Convenio Nacional\",\"universidadId\":" + uni + "}"
    );
    int res = crear(
      "residentes",
      "{\"nombre\":\"Residente test\",\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"LOCAL-TEST\",\"programaId\":" +
        prog +
        "}"
    );
    int serv = crear(
      "servicios",
      "{\"nombre\":\"Servicio local test\",\"cupoSimultaneo\":1,\"cupoMaximoInstalado\":2}"
    );
    int asig = crear(
      "asignaturas",
      "{\"nombre\":\"Asignatura local test\",\"servicioId\":" + serv + "}"
    );
    int user = crear(
      "usuarios",
      "{\"nombre\":\"Usuario\",\"apellido\":\"Local\",\"correo\":\"local-test@example.test\",\"rolId\":2,\"contrasena\":\"LocalTest123!\"}"
    );
    int coord = crear(
      "coordinadores",
      "{\"usuarioId\":" + user + ",\"servicioId\":" + serv + "}"
    );
    String rot = "rotaciones/gestion/" + asig + "/2099-01-01/2099-01-31";
    var creada = enviar(
      "POST",
      "rotaciones/gestion",
      "{\"asignaturaId\":" +
        asig +
        ",\"inicio\":\"2099-01-01\",\"fin\":\"2099-01-31\"}"
    );
    assertThat(creada.statusCode()).as(creada.body()).isEqualTo(201);
    assertThat(
      enviar(
        "POST",
        rot + "/residentes",
        "{\"residenteId\":" + res + "}"
      ).statusCode()
    ).isEqualTo(201);
    assertThat(get(rot).body()).contains("\"cupoDisponible\":0");
    assertThat(
      enviar("POST", rot + "/residentes", "{\"residenteId\":1}").statusCode()
    ).isEqualTo(201);
    assertThat(get(rot + "/residentes?espera=true").body()).contains("Esperando cupo");
    assertThat(
      enviar(
        "PATCH",
        rot + "/residentes/1/estado",
        "{\"estado\":\"Asignado\"}"
      ).statusCode()
    ).isEqualTo(409);

    int res2 = crear(
      "residentes",
      "{\"nombre\":\"Residente test 2\",\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"LOCAL-TEST-2\",\"programaId\":" +
        prog +
        "}"
    );
    assertThat(
      enviar("POST", rot + "/residentes", "{\"residenteId\":" + res2 + "}").statusCode()
    ).isEqualTo(201);
    String servicio = "servicios/" + serv;
    var bajar = enviar(
      "PUT",
      servicio,
      "{\"nombre\":\"Servicio local test\",\"cupoSimultaneo\":0,\"cupoMaximoInstalado\":2}"
    );
    assertThat(bajar.statusCode()).isEqualTo(409);
    assertThat(bajar.body()).contains("mínimo permitido es 1");
    // Ampliar el cupo promueve al primero en espera (residente 1); res2 sigue esperando.
    assertThat(
      enviar(
        "PUT",
        servicio,
        "{\"nombre\":\"Servicio local test\",\"cupoSimultaneo\":2,\"cupoMaximoInstalado\":2}"
      ).statusCode()
    ).isEqualTo(200);
    assertThat(get(rot).body()).contains("\"cupoDisponible\":0");
    assertThat(get(rot + "/residentes?espera=true").body()).contains("Esperando cupo");
    assertThat(
      enviar(
        "PUT",
        servicio,
        "{\"nombre\":\"Servicio local test\",\"cupoSimultaneo\":1,\"cupoMaximoInstalado\":2}"
      ).body()
    ).contains("mínimo permitido es 2");

    // Quitar el cupo a res lo manda a la espera y su cupo lo toma res2.
    assertThat(
      enviar(
        "PATCH",
        rot + "/residentes/" + res + "/estado",
        "{\"estado\":\"Esperando cupo\"}"
      ).statusCode()
    ).isEqualTo(204);
    assertThat(get(rot).body()).contains("\"cupoDisponible\":0");
    assertThat(get(rot + "/residentes?espera=true").body()).contains("\"residenteId\":" + res + ",");
    assertThat(get(rot + "/residentes?espera=true").body()).doesNotContain("\"residenteId\":" + res2 + ",");
    // Quien está en espera se puede eliminar.
    assertThat(enviar("DELETE", rot + "/residentes/" + res, "").statusCode()).isEqualTo(204);
    assertThat(get(rot + "/residentes?espera=true").body()).doesNotContain("Esperando cupo");
    for (String id : new String[] { "1", String.valueOf(res2) }) {
      assertThat(
        enviar(
          "PATCH",
          rot + "/residentes/" + id + "/estado",
          "{\"estado\":\"Cancelado\"}"
        ).statusCode()
      ).isEqualTo(204);
    }
    assertThat(get(rot).body()).contains("\"cupoDisponible\":2");

    assertThat(enviar("DELETE", rot, "").statusCode()).isEqualTo(409);
    for (String ruta : new String[] {
      "universidades/" + uni,
      "programas/" + prog,
      "residentes/" + res,
      "servicios/" + serv,
      "asignaturas/" + asig,
      "usuarios/" + user,
      "coordinadores/" + coord,
    }) {
      assertThat(get(ruta).statusCode()).as(ruta).isEqualTo(200);
      assertThat(
        enviar(
          "PATCH",
          ruta + "/estado",
          "{\"estado\":\"Inactivo\"}"
        ).statusCode()
      )
        .as(ruta)
        .isIn(200, 204);
    }
    assertThat(
      enviar(
        "DELETE",
        "coordinadores/" + coord + "/definitivo",
        ""
      ).statusCode()
    ).isEqualTo(204);
    assertThat(
      enviar("DELETE", "usuarios/" + user + "/definitivo", "").statusCode()
    ).isEqualTo(204);
  }

  @Test
  void restriccionesDeEntradaYDuplicados() throws Exception {
    for (String body : new String[] {
      "{}",
      "{\"nombre\":null,\"pais\":\"CO\"}",
      "{\"nombre\":\"   \",\"pais\":\"CO\"}",
      "{\"nombre\":\"" + "a".repeat(151) + "\",\"pais\":\"CO\"}",
    })
      assertThat(enviar("POST", "universidades", body).statusCode()).isEqualTo(
        400
      );
    for (String cupo : new String[] {
      "null",
      "-1",
      "1.5",
      "2147483648",
      "\"2\"",
    })
      assertThat(
        enviar(
          "POST",
          "servicios",
          "{\"nombre\":\"Inválido\",\"cupoSimultaneo\":" +
            cupo +
            ",\"cupoMaximoInstalado\":3}"
        ).statusCode()
      )
        .as(cupo)
        .isEqualTo(400);
    assertThat(
      enviar(
        "POST",
        "universidades",
        "{\"nombre\":\"Universidad de demostración\",\"pais\":\"CO\"}"
      ).statusCode()
    ).isEqualTo(409);
    assertThat(
      enviar(
        "POST",
        "asignaturas",
        "{\"nombre\":\"Sin servicio\",\"servicioId\":999999}"
      ).statusCode()
    ).isEqualTo(400);
  }

  @Test
  void busquedasYFiltrosEnTodosLosModulos() throws Exception {
    for (String ruta : new String[] {
      "universidades?q=demo",
      "programas?q=demo",
      "residentes?q=demo&periodo=2099-1",
      "usuarios?q=local",
      "servicios?q=demo",
      "asignaturas?q=demo",
      "coordinadores?q=local",
      "rotaciones?q=demo",
      "rotaciones/gestion?q=demo",
      "historial?desde=2020-01-01&hasta=2100-01-01",
      "dashboard?periodo=2099-1",
    }) {
      var respuesta = get(ruta);
      assertThat(respuesta.statusCode())
        .as(ruta + ": " + respuesta.body())
        .isEqualTo(200);
    }
  }

  @Test
  void rechazaNumerosEnCamposDeTexto() throws Exception {
    for (String valor : new String[] { "123", "1.5", "true" }) {
      assertThat(
        enviar(
          "POST",
          "universidades",
          "{\"nombre\":" + valor + ",\"pais\":\"CO\"}"
        ).statusCode()
      ).isEqualTo(400);
    }
  }

  @org.springframework.beans.factory.annotation.Autowired
  org.springframework.jdbc.core.JdbcTemplate jdbc;

  @Test
  void esquemaLocalConservaRestricciones() {
    for (String sql : new String[] {
      "insert into universidad(nombre_universidad,pais) values(null,'CO')",
      "insert into universidad(nombre_universidad,pais) values('Universidad de demostración','CO')",
      "insert into programa(id_universidad,nombre_programa) values(999999,'Referencia inválida')",
      "insert into servicio(nombre,cupo_simultaneo,cupo_maximo_instalado) values('Inválido',4,2)",
      "insert into universidad(nombre_universidad,pais,estado) values('Inválida','CO','Otro')",
    })
      assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(
        org.springframework.dao.DataIntegrityViolationException.class
      );
  }
}
