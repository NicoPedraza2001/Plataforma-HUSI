package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.dto.request.AsignaturaRequest;
import co.edu.javeriana.husi.dto.request.ResidenteRequest;
import co.edu.javeriana.husi.dto.request.ServicioRequest;
import co.edu.javeriana.husi.dto.request.UniversidadRequest;
import jakarta.validation.Validator;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/** CU-04 Gestionar los datos base (RF-07 a RF-12). */
class CU04DatosBaseIntegrationTests extends BaseCasoDeUso {

  @Autowired
  Validator validador;

  private Integer idUniversidad(String nombre) {
    return jdbc.queryForObject(
      "select id_universidad from universidad where nombre_universidad=?",
      Integer.class,
      nombre
    );
  }

  private ResidenteRequest residenteDelPrograma(Integer programaId, String documento) {
    return new ResidenteRequest("Residente de prueba", "CC", documento, null, programaId);
  }

  // ---------- Universidades ----------

  @Test
  @DisplayName("CU04-U-EX3 Una universidad inexistente o con id inválido da un error explícito")
  void unaUniversidadInexistenteDaUnErrorExplicito() {
    debeFallar(HttpStatus.NOT_FOUND, "No se encontró la universidad", () ->
      universidades.consultar(ID_INEXISTENTE)
    );
    debeFallar(HttpStatus.BAD_REQUEST, "no es válida", () -> universidades.consultar(0));
  }

  // ---------- Programas ----------

  @Test
  @DisplayName("CU04-P-EX4 Solo se ofrecen programas activos de universidades activas")
  void soloSeOfrecenProgramasActivosDeUniversidadesActivas() {
    var programaInactivo = programaNuevo();
    var programaDeUniversidadInactiva = programaNuevo();
    programas.cambiarEstado(programaInactivo.id(), "Inactivo");
    universidades.cambiarEstado(idUniversidad(programaDeUniversidadInactiva.universidad()), "Inactivo");

    assertThat(programas.listar()).extracting("id").doesNotContain(programaInactivo.id());
    assertThat(universidades.catalogo())
      .extracting("nombre")
      .doesNotContain(programaDeUniversidadInactiva.universidad());
    debeFallar(HttpStatus.BAD_REQUEST, "activos", () ->
      residentes.crear(residenteDelPrograma(programaInactivo.id(), UUID.randomUUID().toString()))
    );
    debeFallar(HttpStatus.BAD_REQUEST, "activos", () ->
      residentes.crear(
        residenteDelPrograma(programaDeUniversidadInactiva.id(), UUID.randomUUID().toString())
      )
    );
  }

  @Test
  @DisplayName("CU04-P-EX5 La carga de programas rechaza una universidad desconocida o inactiva")
  void laCargaDeProgramasRechazaUniversidadesDesconocidasOInactivas() {
    debeFallar(HttpStatus.BAD_REQUEST, "no encontrada o inactiva", () ->
      programas.validarImportacion("Convenio Nacional", unico("Universidad desconocida"))
    );

    var programa = programaNuevo();
    universidades.cambiarEstado(idUniversidad(programa.universidad()), "Inactivo");
    debeFallar(HttpStatus.BAD_REQUEST, "no encontrada o inactiva", () ->
      programas.validarImportacion("Convenio Nacional", programa.universidad())
    );

    debeFallar(HttpStatus.BAD_REQUEST, "Falta la universidad", () ->
      programas.validarImportacion("Convenio Nacional", "   ")
    );
  }

  // ---------- Servicios y cupos ----------

  @Test
  @DisplayName("CU04-S-FA1 Bajar el cupo justo hasta el número de asignados se permite")
  void bajarElCupoHastaLosAsignadosSePermite() {
    var servicio = servicioConCupo(3);
    var rotacion = crearRotacion(asignaturaDe(servicio.id()), INICIO_FUTURO, FIN_FUTURO);
    asignarResidentesNuevos(rotacion, 2);

    var actualizado = servicios.actualizar(
      servicio.id(),
      new ServicioRequest(servicio.nombre(), null, 2, servicio.cupoMaximoInstalado())
    );

    assertThat(actualizado.cupoSimultaneo()).isEqualTo(2);
    assertThat(cupoLibre(rotacion)).isZero();
  }

  @Test
  @DisplayName("CU04-S-EX3 Un cupo negativo se rechaza en el servicio y en la validación del formulario")
  void unCupoNegativoSeRechaza() {
    debeFallar(HttpStatus.BAD_REQUEST, "enteros no negativos", () ->
      servicios.crear(new ServicioRequest(unico("Servicio"), null, -1, 5))
    );
    assertThat(
      validador.validate(new ServicioRequest(unico("Servicio"), null, -1, 5))
    ).isNotEmpty();
    assertThat(
      validador.validate(new ServicioRequest(unico("Servicio"), null, 6, 5))
    ).isNotEmpty();
  }

  // ---------- Asignaturas ----------

  @Test
  @DisplayName("CU04-A-FA2 Una asignatura con rotaciones se puede renombrar y cambiar de estado")
  void unaAsignaturaConRotacionesSePuedeRenombrarYCambiarDeEstado() {
    var servicio = servicioConCupo(2);
    Integer asignaturaId = asignaturaDe(servicio.id());
    crearRotacion(asignaturaId, INICIO_FUTURO, FIN_FUTURO);
    String nombreNuevo = unico("Asignatura renombrada");

    var renombrada = asignaturas.actualizar(
      asignaturaId,
      new AsignaturaRequest(nombreNuevo, servicio.id())
    );

    assertThat(renombrada.nombre()).isEqualTo(nombreNuevo);
    assertThat(asignaturas.cambiarEstado(asignaturaId, "Inactivo").activo()).isFalse();
    assertThat(asignaturas.cambiarEstado(asignaturaId, "Activo").activo()).isTrue();
  }

  // ---------- Estados (RF-12) ----------

  @Test
  @DisplayName("CU04-C-EX1 Los registros inactivos no aparecen en los catálogos de los formularios")
  void losInactivosNoAparecenEnLosCatalogos() {
    var servicioInactivo = servicioConCupo(2);
    var servicioConAsignaturaInactiva = servicioConCupo(2);
    Integer asignaturaInactiva = asignaturaDe(servicioConAsignaturaInactiva.id());
    String nombreAsignatura = asignaturas.consultar(asignaturaInactiva).nombre();
    String universidad = universidades
      .crear(new UniversidadRequest(unico("Universidad"), "Colombia"))
      .nombre();

    servicios.cambiarEstado(servicioInactivo.id(), "Inactivo");
    asignaturas.cambiarEstado(asignaturaInactiva, "Inactivo");
    universidades.cambiarEstado(idUniversidad(universidad), "Inactivo");

    assertThat(servicios.catalogo(true)).extracting("nombre").doesNotContain(servicioInactivo.nombre());
    assertThat(servicios.catalogo(true)).extracting("nombre").contains(servicioConAsignaturaInactiva.nombre());
    assertThat(universidades.catalogo()).extracting("nombre").doesNotContain(universidad);
    assertThat(rotaciones.catalogos().get("asignaturas").toString()).doesNotContain(nombreAsignatura);
  }
}
