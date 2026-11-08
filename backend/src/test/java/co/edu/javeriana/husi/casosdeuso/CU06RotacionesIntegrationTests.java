package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.dto.request.RotacionRequest;
import co.edu.javeriana.husi.model.entity.RotacionId;
import co.edu.javeriana.husi.util.Calendario;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** CU-06 Gestionar rotaciones (RF-21 a RF-25). */
class CU06RotacionesIntegrationTests extends BaseCasoDeUso {

  @Test
  @DisplayName("CU06-FP3 Los cupos libres son la capacidad menos los asignados, y se avisa si está llena")
  void losCuposLibresSonLaCapacidadMenosLosAsignados() {
    var rotacion = rotacionConCupo(2);
    assertThat(cupoLibre(rotacion)).isEqualTo(2);
    assertThat(rotaciones.consultar(rotacion).disponibilidad()).isEqualTo("Disponible");

    asignarResidentesNuevos(rotacion, 1);
    assertThat(cupoLibre(rotacion)).isEqualTo(1);

    asignarResidentesNuevos(rotacion, 1);
    assertThat(cupoLibre(rotacion)).isZero();
    assertThat(rotaciones.consultar(rotacion).disponibilidad()).isEqualTo("Cupo Lleno");
  }

  @Test
  @DisplayName("CU06-FA2 Una rotación que inicia hoy se permite")
  void unaRotacionQueIniciaHoySePermite() {
    LocalDate hoy = Calendario.hoy();
    var creada = rotaciones.crear(new RotacionRequest(asignaturaConCupo(2), hoy, hoy));
    assertThat(creada.finalizada()).isFalse();
  }

  @Test
  @DisplayName("CU06-EX3 No se crea una rotación de una asignatura o un servicio inactivos")
  void noSeCreaDeAsignaturaOServicioInactivos() {
    Integer asignaturaInactiva = asignaturaConCupo(2);
    asignaturas.cambiarEstado(asignaturaInactiva, "Inactivo");
    debeFallar(HttpStatus.BAD_REQUEST, "inactivos", () ->
      rotaciones.crear(new RotacionRequest(asignaturaInactiva, INICIO_FUTURO, FIN_FUTURO))
    );

    var servicioInactivo = servicioConCupo(2);
    Integer asignaturaDeServicioInactivo = asignaturaDe(servicioInactivo.id());
    servicios.cambiarEstado(servicioInactivo.id(), "Inactivo");
    debeFallar(HttpStatus.BAD_REQUEST, "inactivos", () ->
      rotaciones.crear(new RotacionRequest(asignaturaDeServicioInactivo, INICIO_FUTURO, FIN_FUTURO))
    );
  }

  @Test
  @DisplayName("CU06-EX4 Las fechas fuera de 1900 a 9999 responden 400")
  void lasFechasFueraDeRangoResponden400() {
    Integer asignatura = asignaturaConCupo(2);
    debeFallar(HttpStatus.BAD_REQUEST, "entre 1900 y 9999", () ->
      rotaciones.crear(
        new RotacionRequest(asignatura, LocalDate.of(2091, 1, 1), LocalDate.of(10000, 1, 1))
      )
    );
  }

  @Test
  @DisplayName("CU06-EX5 Cambiar la asignatura de una rotación responde 400")
  void cambiarLaAsignaturaResponde400() {
    var rotacion = rotacionConCupo(2);
    Integer otraAsignatura = asignaturaConCupo(2);
    debeFallar(HttpStatus.BAD_REQUEST, "asignatura no puede cambiarse", () ->
      rotaciones.editar(rotacion, new RotacionRequest(otraAsignatura, INICIO_FUTURO, FIN_FUTURO))
    );
  }

  @Test
  @DisplayName("CU06-EX8 Un filtro desconocido o mal formado responde 400")
  void unFiltroInvalidoResponde400() {
    debeFallar(HttpStatus.BAD_REQUEST, "desconocido", () ->
      rotaciones.listar(Map.of("noExiste", "x"))
    );
    debeFallar(HttpStatus.BAD_REQUEST, "no es válida", () ->
      rotaciones.listar(Map.of("desde", "01/02/2091"))
    );
    debeFallar(HttpStatus.BAD_REQUEST, "no puede ser anterior", () ->
      rotaciones.listar(Map.of("desde", "2091-02-01", "hasta", "2091-01-01"))
    );
    debeFallar(HttpStatus.BAD_REQUEST, "estado no es válido", () ->
      rotaciones.listar(Map.of("activa", "quizas"))
    );
    debeFallar(HttpStatus.BAD_REQUEST, "200 caracteres", () ->
      rotaciones.listar(Map.of("q", "x".repeat(201)))
    );
  }

  @Test
  @DisplayName("CU06-EX9 Una rotación inexistente responde 404")
  void unaRotacionInexistenteResponde404() {
    var inexistente = new RotacionId(ID_INEXISTENTE, INICIO_FUTURO, FIN_FUTURO);
    debeFallar(HttpStatus.NOT_FOUND, "No se encontró la rotación", () ->
      rotaciones.consultar(inexistente)
    );
    debeFallar(HttpStatus.NOT_FOUND, "No se encontró la rotación", () ->
      rotaciones.eliminar(inexistente)
    );
  }
}
