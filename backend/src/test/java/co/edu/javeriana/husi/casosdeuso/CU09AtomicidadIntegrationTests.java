package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

import co.edu.javeriana.husi.dto.request.CargaInicialRequest;
import co.edu.javeriana.husi.service.CargaInicialService;
import co.edu.javeriana.husi.service.RotacionResidenteService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * CU09-EX8 (RF-20): si falla la operación final, no queda nada de lo que esa operación creó. No es
 * transaccional a propósito: la confirmación debe abrir y revertir su propia transacción.
 */
@SpringBootTest
@WithUserDetails("admin@husi.com")
class CU09AtomicidadIntegrationTests {

  @Autowired
  CargaInicialService cargaInicial;

  @Autowired
  JdbcTemplate jdbc;

  @MockitoSpyBean
  RotacionResidenteService asignaciones;

  private final String marca = UUID.randomUUID().toString();

  private CargaInicialRequest.Fila fila(int numeroDeFila, String documento) {
    return new CargaInicialRequest.Fila(
      numeroDeFila,
      "Universidad " + marca,
      "Colombia",
      "Convenio Nacional",
      "Residente " + documento,
      "CC",
      documento,
      "",
      "Servicio " + marca,
      "2",
      "2",
      "Asignatura " + marca,
      "2092-02-01",
      "2092-02-28"
    );
  }

  private int contar(String consulta, Object parametro) {
    return jdbc.queryForObject(consulta, Integer.class, parametro);
  }

  @Test
  @DisplayName("CU09-EX8 Si falla la operación final no queda ninguna rotación ni asignación")
  void siFallaLaOperacionFinalNoQuedaNada() {
    var filas = List.of(fila(2, marca + "-1"), fila(3, marca + "-2"));
    var revision = cargaInicial.validar(new CargaInicialRequest(filas, true, null));
    assertThat(revision.validos()).as(revision.toString()).isEqualTo(2);
    long movimientosAntes = jdbc.queryForObject(
      "select count(*) from historial_auditoria",
      Long.class
    );

    // La primera asignación se guarda de verdad; la segunda falla en plena confirmación.
    doCallRealMethod()
      .doThrow(new IllegalStateException("Fallo simulado de la base de datos"))
      .when(asignaciones)
      .asignarHistorica(any(), any());

    assertThatThrownBy(() ->
      cargaInicial.confirmar(new CargaInicialRequest(filas, true, revision.validacion()))
    ).hasMessageContaining("Fallo simulado");

    assertThat(
      contar("select count(*) from servicio where nombre=?", "Servicio " + marca)
    ).isZero();
    assertThat(
      contar(
        "select count(*) from asignatura where nombre_asignatura=?",
        "Asignatura " + marca
      )
    ).isZero();
    assertThat(
      contar(
        "select count(*) from rotacion_residente rr join residente r on r.id_residente=rr.id_residente where r.numero_documento like ?",
        marca + "%"
      )
    ).isZero();
    assertThat(
      contar("select count(*) from residente where numero_documento like ?", marca + "%")
    ).isZero();
    assertThat(
      jdbc.queryForObject("select count(*) from historial_auditoria", Long.class)
    ).isEqualTo(movimientosAntes);
  }
}
