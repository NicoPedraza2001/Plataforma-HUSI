package co.edu.javeriana.husi.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;

/** T-03: los errores de la base se traducen a mensajes para el usuario, sin detalles internos. */
class ApiExceptionHandlerTest {

  private static final String DETALLE_INTERNO = "violates constraint fk_tabla in select secreto";

  private final ApiExceptionHandler manejador = new ApiExceptionHandler();

  @ParameterizedTest(name = "SQLState {0} responde {1}")
  @CsvSource({
    "23502,400",
    "23514,400",
    "22001,400",
    "22003,400",
    "22018,400",
    "23505,409",
    "23503,409",
  })
  @DisplayName("T-03 Cada violación de integridad responde según su causa, sin detalles internos")
  void cadaViolacionRespondeSegunSuCausa(String estadoSql, int estadoHttp) {
    var respuesta = manejador.manejarErrorDeIntegridad(
      new DataIntegrityViolationException(
        DETALLE_INTERNO,
        new SQLException(DETALLE_INTERNO, estadoSql)
      )
    );

    assertThat(respuesta.getStatusCode().value()).isEqualTo(estadoHttp);
    assertThat(respuesta.getBody().mensaje()).doesNotContain("fk_tabla", "select");
  }

  @Test
  @DisplayName("T-03 Un error de base de datos responde 503 sin detalles internos")
  void unErrorDeBaseResponde503() {
    var respuesta = manejador.manejarErrorDeBaseDeDatos(new QueryTimeoutException(DETALLE_INTERNO));

    assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(respuesta.getBody().mensaje()).doesNotContain("fk_tabla", "select");
  }

  @Test
  @DisplayName("T-03 Un error de negocio conserva su estado y su mensaje")
  void unErrorDeNegocioConservaEstadoYMensaje() {
    var respuesta = manejador.manejarErrorDeNegocio(
      new NegocioException(HttpStatus.CONFLICT, "Ya existe.")
    );

    assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(respuesta.getBody().mensaje()).isEqualTo("Ya existe.");
  }
}
