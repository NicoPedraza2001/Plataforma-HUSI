package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.dto.request.ServicioRequest;
import co.edu.javeriana.husi.dto.request.TrasladoEsperaRequest;
import co.edu.javeriana.husi.dto.response.EsperaServicioResponse;
import co.edu.javeriana.husi.model.entity.RotacionId;
import co.edu.javeriana.husi.util.Calendario;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** CU-08 Atender la lista de espera (RF-28, RF-29). */
class CU08ListaEsperaIntegrationTests extends BaseCasoDeUso {

  private static final LocalDate INICIO_DESTINO = LocalDate.of(2091, 3, 1);
  private static final LocalDate FIN_DESTINO = LocalDate.of(2091, 3, 31);

  private TrasladoEsperaRequest trasladoDesde(RotacionId origen, int... residentesAMover) {
    return new TrasladoEsperaRequest(
      java.util.Arrays.stream(residentesAMover)
        .mapToObj(residente ->
          new TrasladoEsperaRequest.Origen(
            residente,
            origen.getAsignaturaId(),
            origen.getInicio(),
            origen.getFin()
          )
        )
        .toList()
    );
  }

  /** Otra rotación de la misma asignatura, y por tanto del mismo servicio y cupo. */
  private RotacionId rotacionDestinoDe(RotacionId origen) {
    return crearRotacion(origen.getAsignaturaId(), INICIO_DESTINO, FIN_DESTINO);
  }

  @Test
  @DisplayName("CU08-FA1 Aumentar el cupo promueve a varios residentes en orden de llegada")
  void aumentarElCupoPromueveAVariosEnOrden() {
    var servicio = servicioConCupo(1);
    var rotacion = crearRotacion(asignaturaDe(servicio.id()), INICIO_FUTURO, FIN_FUTURO);
    var ids = asignarResidentesNuevos(rotacion, 4);

    servicios.actualizar(
      servicio.id(),
      new ServicioRequest(servicio.nombre(), null, 3, servicio.cupoMaximoInstalado())
    );

    assertThat(estadoDe(rotacion, ids.get(1))).isEqualTo("Asignado");
    assertThat(estadoDe(rotacion, ids.get(2))).isEqualTo("Asignado");
    assertThat(estadoDe(rotacion, ids.get(3))).isEqualTo("Esperando cupo");
    assertThat(cupoLibre(rotacion)).isZero();
  }

  @Test
  @DisplayName("CU08-FA4 La lista de traslado marca a quien ya tiene una rotación que se cruza")
  void laListaDeTrasladoMarcaLosCruces() {
    Integer asignatura = asignaturaConCupo(1);
    var origen = crearRotacion(asignatura, INICIO_FUTURO, FIN_FUTURO);
    var ids = asignarResidentesNuevos(origen, 3);
    int conCruce = ids.get(1);
    int sinCruce = ids.get(2);
    var otraAsignada = crearRotacion(asignatura, INICIO_DESTINO, FIN_DESTINO);
    asignaciones.asignar(otraAsignada, conCruce);

    var destino = crearRotacion(asignatura, INICIO_DESTINO.plusDays(10), FIN_DESTINO.plusDays(10));
    var espera = asignaciones.listarEsperaDelServicio(destino);

    assertThat(espera)
      .filteredOn(fila -> fila.residenteId() == conCruce)
      .extracting(EsperaServicioResponse::cruce)
      .containsExactly(true);
    assertThat(espera)
      .filteredOn(fila -> fila.residenteId() == sinCruce)
      .extracting(EsperaServicioResponse::cruce)
      .containsExactly(false);
  }

  @Test
  @DisplayName("CU08-EX1 Un residente inactivo se salta y el cupo va al siguiente elegible")
  void unResidenteInactivoSeSalta() {
    var rotacion = rotacionConCupo(1);
    var ids = asignarResidentesNuevos(rotacion, 3);
    int primeroEnEspera = ids.get(1);
    int siguiente = ids.get(2);
    residentes.cambiarEstado(primeroEnEspera, "Inactivo");

    asignaciones.cambiarEstado(rotacion, ids.get(0), "Cancelado");

    assertThat(estadoDe(rotacion, primeroEnEspera)).isEqualTo("Esperando cupo");
    assertThat(estadoDe(rotacion, siguiente)).isEqualTo("Asignado");
  }

  @Test
  @DisplayName("CU08-EX3 En una rotación finalizada no se promueve a nadie")
  void enUnaRotacionFinalizadaNoSePromueve() {
    LocalDate hoy = Calendario.hoy();
    var finalizada = insertarRotacion(asignaturaConCupo(1), hoy.minusDays(30), hoy.minusDays(1));
    int asignado = residenteNuevo();
    int enEspera = residenteNuevo();
    asignaciones.asignarHistorica(finalizada, asignado);
    asignaciones.asignarHistorica(finalizada, enEspera);
    assertThat(estadoDe(finalizada, enEspera)).isEqualTo("Esperando cupo");

    // El servicio lo impediría por ser de solo lectura: se libera el cupo directo en la base.
    jdbc.update("update rotacion_residente set estado='Cancelado' where id_residente=?", asignado);

    assertThat(estadoDe(finalizada, enEspera)).isEqualTo("Esperando cupo");
  }

  @Test
  @DisplayName("CU08-EX4 Con la asignatura inactiva no se promueve a nadie")
  void conAsignaturaInactivaNoSePromueve() {
    var rotacion = rotacionConCupo(1);
    var ids = asignarResidentesNuevos(rotacion, 2);
    asignaturas.cambiarEstado(rotacion.getAsignaturaId(), "Inactivo");

    asignaciones.cambiarEstado(rotacion, ids.get(0), "Cancelado");

    assertThat(estadoDe(rotacion, ids.get(1))).isEqualTo("Esperando cupo");
  }

  @Test
  @DisplayName("CU08-EX4 Con el servicio inactivo no se promueve a nadie")
  void conServicioInactivoNoSePromueve() {
    var servicio = servicioConCupo(1);
    var rotacion = crearRotacion(asignaturaDe(servicio.id()), INICIO_FUTURO, FIN_FUTURO);
    var ids = asignarResidentesNuevos(rotacion, 2);
    servicios.cambiarEstado(servicio.id(), "Inactivo");

    asignaciones.cambiarEstado(rotacion, ids.get(0), "Cancelado");

    assertThat(estadoDe(rotacion, ids.get(1))).isEqualTo("Esperando cupo");
  }

  @Test
  @DisplayName("CU08-EX6 Un traslado con un residente repetido responde 400")
  void unTrasladoConResidenteRepetidoResponde400() {
    var origen = rotacionConCupo(1);
    int enEspera = asignarResidentesNuevos(origen, 2).get(1);
    var destino = rotacionDestinoDe(origen);

    debeFallar(HttpStatus.BAD_REQUEST, "solo puede trasladarse una vez", () ->
      asignaciones.trasladarEspera(destino, trasladoDesde(origen, enEspera, enEspera))
    );
  }

  @Test
  @DisplayName("CU08-EX7 No se traslada a quien espera en otro servicio: 409 y no cambia nada")
  void noSeTrasladaDesdeOtroServicio() {
    var origenDeOtroServicio = rotacionConCupo(1);
    int enEspera = asignarResidentesNuevos(origenDeOtroServicio, 2).get(1);
    var destino = rotacionConCupo(1);

    debeFallar(HttpStatus.CONFLICT, "otro servicio", () ->
      asignaciones.trasladarEspera(destino, trasladoDesde(origenDeOtroServicio, enEspera))
    );

    assertThat(estadoDe(origenDeOtroServicio, enEspera)).isEqualTo("Esperando cupo");
    assertThat(cupoLibre(destino)).isEqualTo(1);
  }

  @Test
  @DisplayName("CU08-EX7 No se traslada a quien ya no está en espera: 409")
  void noSeTrasladaAQuienYaNoEspera() {
    var origen = rotacionConCupo(1);
    int asignado = asignarResidentesNuevos(origen, 1).getFirst();
    var destino = rotacionDestinoDe(origen);

    debeFallar(HttpStatus.CONFLICT, "ya no está en espera", () ->
      asignaciones.trasladarEspera(destino, trasladoDesde(origen, asignado))
    );

    assertThat(estadoDe(origen, asignado)).isEqualTo("Asignado");
  }

  @Test
  @DisplayName("CU08-EX8 No se traslada a quien ya está en el destino: 409")
  void noSeTrasladaAQuienYaEstaEnElDestino() {
    Integer asignatura = asignaturaConCupo(2);
    var origen = crearRotacion(asignatura, INICIO_FUTURO, FIN_FUTURO);
    var ids = asignarResidentesNuevos(origen, 3);
    int enEspera = ids.get(2);
    var destino = crearRotacion(asignatura, INICIO_DESTINO, FIN_DESTINO);
    asignaciones.asignar(destino, enEspera);

    debeFallar(HttpStatus.CONFLICT, "ya está relacionado con esta rotación", () ->
      asignaciones.trasladarEspera(destino, trasladoDesde(origen, enEspera))
    );
  }

  @Test
  @DisplayName("CU08-EX8 No se traslada a un residente inactivo: 409")
  void noSeTrasladaAUnResidenteInactivo() {
    var origen = rotacionConCupo(1);
    int enEspera = asignarResidentesNuevos(origen, 2).get(1);
    residentes.cambiarEstado(enEspera, "Inactivo");
    var destino = rotacionDestinoDe(origen);

    debeFallar(HttpStatus.CONFLICT, "debe estar activo", () ->
      asignaciones.trasladarEspera(destino, trasladoDesde(origen, enEspera))
    );
  }

  @Test
  @DisplayName("CU08-EX9 No se traslada a un destino inactivo: 409")
  void noSeTrasladaAUnDestinoInactivo() {
    var origen = rotacionConCupo(1);
    int enEspera = asignarResidentesNuevos(origen, 2).get(1);
    var destino = rotacionDestinoDe(origen);
    rotaciones.cambiarEstado(destino, false);

    debeFallar(HttpStatus.CONFLICT, "deben estar activos", () ->
      asignaciones.trasladarEspera(destino, trasladoDesde(origen, enEspera))
    );
  }

  @Test
  @DisplayName("CU08-EX9 No se traslada a un destino finalizado: 409")
  void noSeTrasladaAUnDestinoFinalizado() {
    LocalDate hoy = Calendario.hoy();
    var origen = rotacionConCupo(1);
    int enEspera = asignarResidentesNuevos(origen, 2).get(1);
    var finalizado = insertarRotacion(
      origen.getAsignaturaId(),
      hoy.minusDays(30),
      hoy.minusDays(1)
    );

    debeFallar(HttpStatus.CONFLICT, "finalizó", () ->
      asignaciones.trasladarEspera(finalizado, trasladoDesde(origen, enEspera))
    );
  }

  @Test
  @DisplayName("CU08-FP2 La lista de espera se consulta aparte y sigue el orden de llegada")
  void laListaDeEsperaSigueElOrdenDeLlegada() {
    var rotacion = rotacionConCupo(1);
    var ids = asignarResidentesNuevos(rotacion, 4);

    var espera = asignaciones.listar(rotacion, 1, true).contenido();

    assertThat(espera).extracting("residenteId").containsExactly(ids.get(1), ids.get(2), ids.get(3));
    assertThat(List.of(espera.getFirst().estado())).containsExactly("Esperando cupo");
  }
}
