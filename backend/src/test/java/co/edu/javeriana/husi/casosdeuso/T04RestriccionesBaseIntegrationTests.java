package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.javeriana.husi.InsercionDePrueba;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/**
 * T-04 Restricciones de la propia base de datos (RBD-04, RBD-05, RBD-07), comprobadas sin pasar por
 * los servicios: protegen los datos aunque falle una validación del backend. Cada prueba provoca
 * una sola violación, porque PostgreSQL aborta la transacción después del primer error.
 */
class T04RestriccionesBaseIntegrationTests extends BaseCasoDeUso {

  private void debeRechazarLaBase(String sentencia, Object... parametros) {
    assertThatThrownBy(() -> jdbc.update(sentencia, parametros)).isInstanceOf(
      DataAccessException.class
    );
  }

  private int insertarServicio(int cupoSimultaneo, int cupoMaximo) {
    return InsercionDePrueba.insertarYObtenerId(
      jdbc,
      "id_servicio",
      "insert into servicio(nombre,cupo_maximo_instalado,cupo_simultaneo) values(?,?,?)",
      unico("Servicio"),
      cupoMaximo,
      cupoSimultaneo
    );
  }

  @Test
  @DisplayName("T-04 RBD-04 La base no repite el correo de un usuario")
  void noRepiteElCorreo() {
    String correo = UUID.randomUUID() + "@example.test";
    String insertar =
      "insert into usuario(id_rol,nombre,apellido,correo,contrasena) values(?,'Prueba','Base',?,'clave')";
    jdbc.update(insertar, idRol("Administrador"), correo);
    debeRechazarLaBase(insertar, idRol("Administrador"), correo);
  }

  @Test
  @DisplayName("T-04 RBD-04 La base no repite el documento de un residente")
  void noRepiteElDocumento() {
    int programa = programaNuevo().id();
    String documento = UUID.randomUUID().toString();
    String insertar =
      "insert into residente(id_programa,id_tipo_documento,nombre_completo,numero_documento) values(?,1,'Prueba',?)";
    jdbc.update(insertar, programa, documento);
    debeRechazarLaBase(insertar, programa, documento);
  }

  @Test
  @DisplayName("T-04 RBD-04 La base no repite el nombre de una universidad")
  void noRepiteLaUniversidad() {
    debeRechazarLaBase(
      "insert into universidad(nombre_universidad,pais) values(?,'Colombia')",
      programaNuevo().universidad()
    );
  }

  @Test
  @DisplayName("T-04 RBD-04 La base no repite un programa en la misma universidad")
  void noRepiteElProgramaEnLaUniversidad() {
    var programa = programaNuevo();
    Integer universidad = jdbc.queryForObject(
      "select id_universidad from programa where id_programa=?",
      Integer.class,
      programa.id()
    );
    debeRechazarLaBase(
      "insert into programa(id_universidad,nombre_programa) values(?,?)",
      universidad,
      programa.nombre()
    );
  }

  @Test
  @DisplayName("T-04 RBD-04 La base no repite una asignatura en el mismo servicio")
  void noRepiteLaAsignaturaEnElServicio() {
    int servicio = insertarServicio(1, 1);
    String insertar = "insert into asignatura(id_servicio,nombre_asignatura) values(?,'Repetida')";
    jdbc.update(insertar, servicio);
    debeRechazarLaBase(insertar, servicio);
  }

  @Test
  @DisplayName("T-04 RBD-04 La base no repite la rotación de una asignatura con las mismas fechas")
  void noRepiteLaRotacion() {
    var rotacion = insertarRotacion(asignaturaConCupo(1), INICIO_FUTURO, FIN_FUTURO);
    debeRechazarLaBase(
      "insert into rotacion(id_asignatura,fecha_inicio,fecha_fin) values(?,?,?)",
      rotacion.getAsignaturaId(),
      rotacion.getInicio(),
      rotacion.getFin()
    );
  }

  @Test
  @DisplayName("T-04 RBD-05 La base rechaza un cupo simultáneo mayor que el máximo instalado")
  void rechazaCupoMayorQueElMaximo() {
    debeRechazarLaBase(
      "insert into servicio(nombre,cupo_maximo_instalado,cupo_simultaneo) values(?,1,2)",
      unico("Servicio")
    );
  }

  @Test
  @DisplayName("T-04 RBD-05 La base rechaza un cupo simultáneo negativo")
  void rechazaCupoNegativo() {
    debeRechazarLaBase(
      "insert into servicio(nombre,cupo_maximo_instalado,cupo_simultaneo) values(?,1,-1)",
      unico("Servicio")
    );
  }

  @Test
  @DisplayName("T-04 RBD-07 La base rechaza eliminar una universidad con programas")
  void rechazaEliminarUniversidadConProgramas() {
    debeRechazarLaBase(
      "delete from universidad where nombre_universidad=?",
      programaNuevo().universidad()
    );
  }

  @Test
  @DisplayName("T-04 RBD-07 La base rechaza eliminar un programa con residentes")
  void rechazaEliminarProgramaConResidentes() {
    int programa = programaNuevo().id();
    residenteDelPrograma(programa);
    debeRechazarLaBase("delete from programa where id_programa=?", programa);
  }

  @Test
  @DisplayName("T-04 RBD-07 La base rechaza eliminar una asignatura con rotaciones")
  void rechazaEliminarAsignaturaConRotaciones() {
    var rotacion = insertarRotacion(asignaturaConCupo(1), INICIO_FUTURO, FIN_FUTURO);
    debeRechazarLaBase(
      "delete from asignatura where id_asignatura=?",
      rotacion.getAsignaturaId()
    );
  }

  @Test
  @DisplayName("T-04 RBD-07 La base rechaza eliminar un residente con rotaciones")
  void rechazaEliminarResidenteConRotaciones() {
    int residente = asignarResidentesNuevos(rotacionConCupo(1), 1).getFirst();
    debeRechazarLaBase("delete from residente where id_residente=?", residente);
  }
}
