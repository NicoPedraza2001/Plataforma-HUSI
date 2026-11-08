package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.model.entity.RotacionId;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** CU-07 Asignar residentes a una rotación (RF-26, RF-27). */
class CU07AsignacionIntegrationTests extends BaseCasoDeUso {

  private long asignacionesDe(RotacionId rotacion, int residenteId) {
    return jdbc.queryForObject(
      "select count(*) from rotacion_residente where id_residente=? and id_asignatura=? and fecha_inicio=? and fecha_fin=?",
      Long.class,
      residenteId,
      rotacion.getAsignaturaId(),
      rotacion.getInicio(),
      rotacion.getFin()
    );
  }

  @Test
  @DisplayName("CU07-FA6 Se elimina una relación en espera o cancelada, también en rotación inactiva")
  void seEliminaUnaRelacionEnEsperaOCancelada() {
    var rotacion = rotacionConCupo(1);
    var ids = asignarResidentesNuevos(rotacion, 3);
    int asignado = ids.get(0);
    int enEspera = ids.get(1);
    int cancelado = ids.get(2);
    asignaciones.cambiarEstado(rotacion, cancelado, "Cancelado");
    rotaciones.cambiarEstado(rotacion, false);

    asignaciones.eliminar(rotacion, enEspera);
    asignaciones.eliminar(rotacion, cancelado);

    assertThat(asignacionesDe(rotacion, enEspera)).isZero();
    assertThat(asignacionesDe(rotacion, cancelado)).isZero();
    assertThat(asignacionesDe(rotacion, asignado)).isEqualTo(1);
  }

  @Test
  @DisplayName("CU07-EX4 Un residente inactivo no se asigna: 409")
  void unResidenteInactivoNoSeAsigna() {
    var rotacion = rotacionConCupo(2);
    int inactivo = residenteNuevo();
    residentes.cambiarEstado(inactivo, "Inactivo");
    debeFallar(HttpStatus.CONFLICT, "debe estar activo", () ->
      asignaciones.asignar(rotacion, inactivo)
    );
  }

  @Test
  @DisplayName("CU07-EX4 Un residente inexistente no se asigna: 400")
  void unResidenteInexistenteNoSeAsigna() {
    var rotacion = rotacionConCupo(2);
    debeFallar(HttpStatus.BAD_REQUEST, "No se encontró el residente", () ->
      asignaciones.asignar(rotacion, ID_INEXISTENTE)
    );
  }

  @Test
  @DisplayName("CU07-EX5 No se asigna en una rotación de asignatura inactiva: 409")
  void noSeAsignaConAsignaturaInactiva() {
    var rotacion = rotacionConCupo(2);
    int residente = residenteNuevo();
    asignaturas.cambiarEstado(rotacion.getAsignaturaId(), "Inactivo");
    debeFallar(HttpStatus.CONFLICT, "deben estar activos", () ->
      asignaciones.asignar(rotacion, residente)
    );
  }

  @Test
  @DisplayName("CU07-EX5 No se asigna en una rotación de servicio inactivo: 409")
  void noSeAsignaConServicioInactivo() {
    var servicio = servicioConCupo(2);
    var rotacion = crearRotacion(asignaturaDe(servicio.id()), INICIO_FUTURO, FIN_FUTURO);
    int residente = residenteNuevo();
    servicios.cambiarEstado(servicio.id(), "Inactivo");
    debeFallar(HttpStatus.CONFLICT, "deben estar activos", () ->
      asignaciones.asignar(rotacion, residente)
    );
  }

  @Test
  @DisplayName("CU07-EX6 Asignar manualmente sin cupo responde 409 y permanece en su estado")
  void asignarManualmenteSinCupoResponde409() {
    var rotacion = rotacionConCupo(1);
    int enEspera = asignarResidentesNuevos(rotacion, 2).get(1);

    debeFallar(HttpStatus.CONFLICT, "No hay cupos disponibles", () ->
      asignaciones.cambiarEstado(rotacion, enEspera, "Asignado")
    );

    assertThat(estadoDe(rotacion, enEspera)).isEqualTo("Esperando cupo");
  }

  @Test
  @DisplayName("CU07-EX7 Quitar el cupo a quien no está asignado responde 409")
  void quitarElCupoAQuienNoEstaAsignadoResponde409() {
    var rotacion = rotacionConCupo(1);
    int enEspera = asignarResidentesNuevos(rotacion, 2).get(1);
    debeFallar(HttpStatus.CONFLICT, "residente asignado", () ->
      asignaciones.cambiarEstado(rotacion, enEspera, "Esperando cupo")
    );
  }

  @Test
  @DisplayName("CU07-EX8 Un estado inválido responde 400 y una asignación inexistente 404")
  void unEstadoInvalidoOUnaAsignacionInexistenteDanError() {
    var rotacion = rotacionConCupo(2);
    int asignado = asignarResidentesNuevos(rotacion, 1).getFirst();
    int sinAsignacion = residenteNuevo();

    debeFallar(HttpStatus.BAD_REQUEST, "Estado de asignación inválido", () ->
      asignaciones.cambiarEstado(rotacion, asignado, "Aprobado")
    );
    debeFallar(HttpStatus.NOT_FOUND, "No se encontró la asignación", () ->
      asignaciones.cambiarEstado(rotacion, sinAsignacion, "Cancelado")
    );
    debeFallar(HttpStatus.NOT_FOUND, "No se encontró la asignación", () ->
      asignaciones.eliminar(rotacion, sinAsignacion)
    );
  }

  @Test
  @DisplayName("CU07-EX9 En una rotación finalizada no se elimina ninguna relación: 409")
  void enUnaRotacionFinalizadaNoSeElimina() {
    var rotacion = insertarRotacion(
      asignaturaConCupo(2),
      LocalDate.of(2020, 1, 1),
      LocalDate.of(2020, 1, 31)
    );
    int residente = residenteNuevo();
    asignaciones.asignarHistorica(rotacion, residente);
    jdbc.update("update rotacion_residente set estado='Cancelado' where id_residente=?", residente);

    debeFallar(HttpStatus.CONFLICT, "finalizó", () -> asignaciones.eliminar(rotacion, residente));
  }

  @Test
  @DisplayName("CU07-EX11 Cada acción deja un movimiento y una acción rechazada no deja ninguno")
  void cadaAccionDejaUnMovimientoYUnRechazoNinguno() {
    var rotacion = rotacionConCupo(1);
    int residente = residenteNuevo();

    long antesDeAsignar = totalMovimientos();
    asignaciones.asignar(rotacion, residente);
    assertThat(totalMovimientos()).isEqualTo(antesDeAsignar + 1);

    long antesDelRechazo = totalMovimientos();
    debeFallar(HttpStatus.CONFLICT, "ya está relacionado", () ->
      asignaciones.asignar(rotacion, residente)
    );
    assertThat(totalMovimientos()).isEqualTo(antesDelRechazo);

    long antesDeCancelar = totalMovimientos();
    asignaciones.cambiarEstado(rotacion, residente, "Cancelado");
    assertThat(totalMovimientos()).isEqualTo(antesDeCancelar + 1);

    long antesDeEliminar = totalMovimientos();
    asignaciones.eliminar(rotacion, residente);
    assertThat(totalMovimientos()).isEqualTo(antesDeEliminar + 1);
  }
}
