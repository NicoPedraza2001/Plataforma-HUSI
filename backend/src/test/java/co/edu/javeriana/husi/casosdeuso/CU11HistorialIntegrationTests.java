package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.dto.request.AsignaturaRequest;
import co.edu.javeriana.husi.dto.request.ProgramaRequest;
import co.edu.javeriana.husi.dto.request.ResidenteRequest;
import co.edu.javeriana.husi.dto.request.RotacionRequest;
import co.edu.javeriana.husi.dto.request.ServicioRequest;
import co.edu.javeriana.husi.dto.request.UniversidadRequest;
import co.edu.javeriana.husi.dto.request.UsuarioRequest;
import co.edu.javeriana.husi.model.entity.RotacionId;
import co.edu.javeriana.husi.util.Calendario;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** CU-11 Consultar el historial (RF-35, RF-36). */
class CU11HistorialIntegrationTests extends BaseCasoDeUso {

  private static final String CREO = "CREÓ";
  private static final String ACTUALIZO = "ACTUALIZÓ";
  private static final String INACTIVO = "INACTIVÓ";
  private static final String ACTIVO = "ACTIVÓ";
  private static final String ELIMINO = "ELIMINÓ";

  private long ultimoMovimiento() {
    return jdbc.queryForObject(
      "select coalesce(max(id_historial),0) from historial_auditoria",
      Long.class
    );
  }

  private List<String> accionesDesde(long movimiento, String modulo) {
    return jdbc.queryForList(
      "select accion_realizada from historial_auditoria where id_historial>? and entidad_modificada=?",
      String.class,
      movimiento,
      modulo
    );
  }

  private Integer idDelMovimiento(String registro) {
    return jdbc.queryForObject(
      "select max(id_historial) from historial_auditoria where registro=?",
      Integer.class,
      registro
    );
  }

  @Test
  @DisplayName("CU11-FP2 El detalle muestra nombre y correo del responsable y los campos que cambiaron")
  void elDetalleMuestraAlResponsableYLosCambios() {
    var servicio = servicioConCupo(2);
    servicios.actualizar(
      servicio.id(),
      new ServicioRequest(servicio.nombre(), "Descripción nueva", 2, 10)
    );

    var detalle = historial.consultar(idDelMovimiento(servicio.nombre()));

    assertThat(detalle.correoUsuario()).isEqualTo("admin@husi.com");
    assertThat(detalle.usuario()).isNotBlank();
    assertThat(detalle.modulo()).isEqualTo("Servicios");
    assertThat(detalle.accion()).isEqualTo(ACTUALIZO);
    assertThat(detalle.datosAnteriores()).containsEntry("Descripción", "—");
    assertThat(detalle.datosNuevos()).containsEntry("Descripción", "Descripción nueva");
    assertThat(detalle.detalle()).contains("Descripción").contains("Descripción nueva");
  }

  @Test
  @DisplayName("CU11-FA1 Universidades registra crear, actualizar, cambiar estado y eliminar")
  void universidadesRegistraSusAcciones() {
    long inicio = ultimoMovimiento();
    var universidad = universidades.crear(new UniversidadRequest(unico("Universidad"), "Colombia"));
    universidades.actualizar(universidad.id(), new UniversidadRequest(unico("Universidad"), "Colombia"));
    universidades.cambiarEstado(universidad.id(), "Inactivo");
    universidades.cambiarEstado(universidad.id(), "Activo");
    universidades.eliminarDefinitivamente(universidad.id());

    assertThat(accionesDesde(inicio, "Universidades")).containsExactly(
      CREO,
      ACTUALIZO,
      INACTIVO,
      ACTIVO,
      ELIMINO
    );
  }

  @Test
  @DisplayName("CU11-FA1 Programas registra crear, cambiar estado y eliminar")
  void programasRegistraSusAcciones() {
    var universidad = universidades.crear(new UniversidadRequest(unico("Universidad"), "Colombia"));
    long inicio = ultimoMovimiento();
    var programa = programas.crear(new ProgramaRequest("Convenio Nacional", universidad.id()));
    programas.cambiarEstado(programa.id(), "Inactivo");
    programas.eliminarDefinitivamente(programa.id());

    assertThat(accionesDesde(inicio, "Programas")).containsExactly(CREO, INACTIVO, ELIMINO);
  }

  @Test
  @DisplayName("CU11-FA1 Servicios registra crear, actualizar, cambiar estado y eliminar")
  void serviciosRegistraSusAcciones() {
    long inicio = ultimoMovimiento();
    var servicio = servicioConCupo(2);
    servicios.actualizar(servicio.id(), new ServicioRequest(unico("Servicio"), null, 3, 10));
    servicios.cambiarEstado(servicio.id(), "Inactivo");
    servicios.eliminarDefinitivamente(servicio.id());

    assertThat(accionesDesde(inicio, "Servicios")).containsExactly(
      CREO,
      ACTUALIZO,
      INACTIVO,
      ELIMINO
    );
  }

  @Test
  @DisplayName("CU11-FA1 Asignaturas registra crear, actualizar, cambiar estado y eliminar")
  void asignaturasRegistraSusAcciones() {
    var servicio = servicioConCupo(2);
    long inicio = ultimoMovimiento();
    Integer asignatura = asignaturaDe(servicio.id());
    asignaturas.actualizar(asignatura, new AsignaturaRequest(unico("Asignatura"), servicio.id()));
    asignaturas.cambiarEstado(asignatura, "Inactivo");
    asignaturas.eliminarDefinitivamente(asignatura);

    assertThat(accionesDesde(inicio, "Asignaturas")).containsExactly(
      CREO,
      ACTUALIZO,
      INACTIVO,
      ELIMINO
    );
  }

  @Test
  @DisplayName("CU11-FA1 Residentes registra crear, actualizar, cambiar estado y eliminar")
  void residentesRegistraSusAcciones() {
    int programa = programaNuevo().id();
    String documento = UUID.randomUUID().toString();
    long inicio = ultimoMovimiento();
    var residente = residentes.crear(
      new ResidenteRequest("Residente de prueba", "CC", documento, null, programa)
    );
    residentes.actualizar(
      residente.id(),
      new ResidenteRequest("Residente renombrado", "CC", documento, null, programa)
    );
    residentes.cambiarEstado(residente.id(), "Inactivo");
    residentes.eliminarDefinitivamente(residente.id());

    assertThat(accionesDesde(inicio, "Residentes")).containsExactly(
      CREO,
      ACTUALIZO,
      INACTIVO,
      ELIMINO
    );
  }

  @Test
  @DisplayName("CU11-FA1 Rotaciones registra crear, actualizar, cambiar estado y eliminar")
  void rotacionesRegistraSusAcciones() {
    Integer asignatura = asignaturaConCupo(2);
    long inicio = ultimoMovimiento();
    var rotacion = crearRotacion(asignatura, INICIO_FUTURO, FIN_FUTURO);
    var fechasNuevas = new RotacionRequest(asignatura, INICIO_FUTURO, FIN_FUTURO.plusDays(1));
    rotaciones.editar(rotacion, fechasNuevas);
    var rotacionEditada = new RotacionId(asignatura, INICIO_FUTURO, FIN_FUTURO.plusDays(1));
    rotaciones.cambiarEstado(rotacionEditada, false);
    rotaciones.eliminar(rotacionEditada);

    assertThat(accionesDesde(inicio, "Rotaciones")).containsExactly(
      CREO,
      ACTUALIZO,
      INACTIVO,
      ELIMINO
    );
  }

  @Test
  @DisplayName("CU11-FA2 Busca por usuario, registro o detalle y filtra por módulo, acción y fechas")
  void buscaYFiltraLosMovimientos() {
    var servicio = servicioConCupo(2);
    String hoy = Calendario.hoy().toString();

    assertThat(
      consultas.listar("historial", Map.of("q", servicio.nombre())).totalElementos()
    ).isEqualTo(1);
    assertThat(
      consultas
        .listar(
          "historial",
          Map.of("q", servicio.nombre(), "modulo", "Servicios", "accion", CREO, "desde", hoy, "hasta", hoy)
        )
        .totalElementos()
    ).isEqualTo(1);
    assertThat(
      consultas.listar("historial", Map.of("q", servicio.nombre(), "modulo", "Residentes")).totalElementos()
    ).isZero();
    assertThat(
      consultas.listar("historial", Map.of("q", servicio.nombre(), "accion", ELIMINO)).totalElementos()
    ).isZero();
    assertThat(
      consultas
        .listar("historial", Map.of("q", servicio.nombre(), "hasta", LocalDate.of(2000, 1, 1).toString()))
        .totalElementos()
    ).isZero();
  }

  @Test
  @DisplayName("CU11-EX1 Ningún movimiento de usuarios contiene contraseñas ni hashes")
  void ningunMovimientoContieneContrasenas() {
    long inicio = ultimoMovimiento();
    String contrasenaNueva = "Otra-Contrasena-2";
    var creado = coordinadorDe(servicioConCupo(2).id());
    usuarios.actualizar(
      creado.id(),
      new UsuarioRequest(
        "Nombre",
        "Cambiado",
        creado.correo(),
        idRol("Coordinador"),
        contrasenaNueva,
        creado.servicioId()
      )
    );
    usuarios.cambiarEstado(creado.id(), "Inactivo");
    usuarios.actualizar(
      creado.id(),
      new UsuarioRequest("Nombre", "Cambiado", creado.correo(), idRol("Administrador"), null, null)
    );
    usuarios.eliminarDefinitivamente(creado.id());

    var movimientos = jdbc
      .queryForList(
        "select detalle,datos_anteriores,datos_nuevos from historial_auditoria where id_historial>? and entidad_modificada='Usuarios'",
        inicio
      )
      .toString();

    assertThat(movimientos)
      .contains("Se cambió")
      .doesNotContain(CONTRASENA_VALIDA)
      .doesNotContain(contrasenaNueva)
      .doesNotContain("$2a$");
  }

  @Test
  @DisplayName("CU11-EX2 Un movimiento inexistente responde 404")
  void unMovimientoInexistenteResponde404() {
    debeFallar(HttpStatus.NOT_FOUND, "No se encontró el movimiento", () ->
      historial.consultar(ID_INEXISTENTE)
    );
  }

  @Test
  @DisplayName("CU11-EX3 Una operación rechazada no deja movimiento")
  void unaOperacionRechazadaNoDejaMovimiento() {
    var servicio = servicioConCupo(2);
    long antes = totalMovimientos();

    debeFallar(HttpStatus.CONFLICT, "Ya existe un servicio", () ->
      servicios.crear(new ServicioRequest(servicio.nombre(), null, 1, 1))
    );
    debeFallar(HttpStatus.BAD_REQUEST, "Estado inválido", () ->
      servicios.cambiarEstado(servicio.id(), "Suspendido")
    );

    assertThat(totalMovimientos()).isEqualTo(antes);
  }
}
