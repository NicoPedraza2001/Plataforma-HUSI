package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CU-12: el coordinador no puede ampliar su alcance desde la petición (RF-34, RD-04). */
class CU12CoordinadorApiH2IntegrationTests extends BaseCasoDeUsoPorApi {

  @Test
  @DisplayName("CU12-EX3 Los parámetros como ?servicio=otro no amplían el acceso")
  void losParametrosNoAmplianElAcceso() throws Exception {
    int otroServicio = crearServicio();
    String nombreDelOtroServicio = jdbc.queryForObject(
      "select nombre from servicio where id_servicio=?",
      String.class,
      otroServicio
    );
    var coordinador = clienteDe(crearCoordinador());

    for (String ruta : new String[] { "coordinador/residentes", "coordinador/dashboard" }) {
      var sinParametros = coordinador.get(ruta);
      var conServicio = coordinador.get(ruta + "?servicio=" + nombreDelOtroServicio);
      var conServicioId = coordinador.get(ruta + "?servicioId=" + otroServicio);

      assertThat(sinParametros.statusCode()).as(ruta).isEqualTo(200);
      assertThat(conServicio.body()).as(ruta).isEqualTo(sinParametros.body());
      assertThat(conServicioId.body()).as(ruta).isEqualTo(sinParametros.body());
      assertThat(conServicio.body()).as(ruta).doesNotContain(nombreDelOtroServicio);
    }
  }
}
