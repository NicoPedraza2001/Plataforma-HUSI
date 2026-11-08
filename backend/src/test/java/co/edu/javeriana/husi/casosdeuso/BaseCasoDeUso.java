package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.javeriana.husi.InsercionDePrueba;
import co.edu.javeriana.husi.dto.request.AsignaturaRequest;
import co.edu.javeriana.husi.dto.request.RotacionRequest;
import co.edu.javeriana.husi.dto.request.ServicioRequest;
import co.edu.javeriana.husi.dto.request.UsuarioRequest;
import co.edu.javeriana.husi.dto.response.ServicioResponse;
import co.edu.javeriana.husi.dto.response.UsuarioResponse;
import co.edu.javeriana.husi.exception.NegocioException;
import co.edu.javeriana.husi.model.entity.RotacionId;
import co.edu.javeriana.husi.security.CargadorUsuarioAutenticado;
import co.edu.javeriana.husi.service.*;
import java.time.LocalDate;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de las pruebas por caso de uso que llaman a los servicios. Cada prueba corre en una
 * transacción que se revierte al terminar, firmada por el administrador, de modo que no deja datos
 * en la base. Sus subclases {@code *H2IntegrationTests} repiten las mismas pruebas sobre H2.
 */
@SpringBootTest
@Transactional
@WithUserDetails("admin@husi.com")
abstract class BaseCasoDeUso {

  /** Las subclases sobre H2 comparten esta base en memoria y, por tanto, un solo contexto. */
  static final String BASE_H2 =
    "spring.datasource.url=jdbc:h2:mem:casosdeuso;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;OPTIMIZE_REUSE_RESULTS=0;DB_CLOSE_DELAY=-1";

  /** Periodo sin datos reales: las rotaciones de prueba se crean en estas fechas. */
  static final LocalDate INICIO_FUTURO = LocalDate.of(2091, 1, 1);
  static final LocalDate FIN_FUTURO = LocalDate.of(2091, 1, 31);

  /** Id que no existe en ninguna tabla. */
  static final int ID_INEXISTENTE = 999_999;

  static final String CONTRASENA_VALIDA = "Contrasena-Prueba-1";

  @Autowired
  protected JdbcTemplate jdbc;

  @Autowired
  protected UsuarioService usuarios;

  @Autowired
  protected ServicioService servicios;

  @Autowired
  protected AsignaturaService asignaturas;

  @Autowired
  protected UniversidadService universidades;

  @Autowired
  protected ProgramaService programas;

  @Autowired
  protected ResidenteService residentes;

  @Autowired
  protected RotacionService rotaciones;

  @Autowired
  protected RotacionResidenteService asignaciones;

  @Autowired
  protected ConsultaService consultas;

  @Autowired
  protected HistorialAuditoriaService historial;

  @Autowired
  protected CoordinadorService coordinadores;

  @Autowired
  protected DashboardService tablero;

  @Autowired
  private CargadorUsuarioAutenticado cargadorUsuario;

  /** Programa de prueba con los nombres que usan los filtros del tablero. */
  protected record ProgramaDePrueba(int id, String nombre, String universidad) {}

  protected static String unico(String prefijo) {
    return prefijo + " " + UUID.randomUUID();
  }

  /** Comprueba que la acción falla con ese estado HTTP y que el mensaje contiene el texto. */
  protected void debeFallar(HttpStatus estado, String textoDelMensaje, ThrowingCallable accion) {
    assertThatThrownBy(accion)
      .isInstanceOfSatisfying(NegocioException.class, error -> {
        org.assertj.core.api.Assertions.assertThat(error.getStatus()).isEqualTo(estado);
        org.assertj.core.api.Assertions.assertThat(error.getMessage()).contains(textoDelMensaje);
      });
  }

  // ---------- Datos base ----------

  protected ServicioResponse servicioConCupo(int cupoSimultaneo) {
    return servicios.crear(
      new ServicioRequest(unico("Servicio"), null, cupoSimultaneo, Math.max(cupoSimultaneo, 10))
    );
  }

  protected Integer asignaturaDe(Integer servicioId) {
    return asignaturas.crear(new AsignaturaRequest(unico("Asignatura"), servicioId)).id();
  }

  protected Integer asignaturaConCupo(int cupoSimultaneo) {
    return asignaturaDe(servicioConCupo(cupoSimultaneo).id());
  }

  protected ProgramaDePrueba programaNuevo() {
    String universidad = unico("Universidad");
    String programa = unico("Programa");
    int universidadId = InsercionDePrueba.insertarYObtenerId(
      jdbc,
      "id_universidad",
      "insert into universidad(nombre_universidad,pais) values(?,'Colombia')",
      universidad
    );
    int programaId = InsercionDePrueba.insertarYObtenerId(
      jdbc,
      "id_programa",
      "insert into programa(id_universidad,nombre_programa) values(?,?)",
      universidadId,
      programa
    );
    return new ProgramaDePrueba(programaId, programa, universidad);
  }

  protected int residenteDelPrograma(int programaId) {
    return InsercionDePrueba.insertarYObtenerId(
      jdbc,
      "id_residente",
      "insert into residente(id_programa,id_tipo_documento,nombre_completo,numero_documento) values(?,1,?,?)",
      programaId,
      unico("Residente"),
      UUID.randomUUID().toString()
    );
  }

  protected int residenteNuevo() {
    return residenteDelPrograma(programaNuevo().id());
  }

  // ---------- Rotaciones y asignaciones ----------

  protected RotacionId crearRotacion(Integer asignaturaId, LocalDate inicio, LocalDate fin) {
    rotaciones.crear(new RotacionRequest(asignaturaId, inicio, fin));
    return new RotacionId(asignaturaId, inicio, fin);
  }

  /** Rotación futura y activa de un servicio nuevo con ese cupo. */
  protected RotacionId rotacionConCupo(int cupoSimultaneo) {
    return crearRotacion(asignaturaConCupo(cupoSimultaneo), INICIO_FUTURO, FIN_FUTURO);
  }

  /** Sin pasar por el servicio, que no admite fechas pasadas: sirve para rotaciones ya iniciadas. */
  protected RotacionId insertarRotacion(Integer asignaturaId, LocalDate inicio, LocalDate fin) {
    jdbc.update(
      "insert into rotacion(id_asignatura,fecha_inicio,fecha_fin) values (?,?,?)",
      asignaturaId,
      inicio,
      fin
    );
    return new RotacionId(asignaturaId, inicio, fin);
  }

  /** Asigna residentes nuevos, en orden de llegada, y devuelve sus ids. */
  protected java.util.List<Integer> asignarResidentesNuevos(RotacionId rotacion, int cantidad) {
    var ids = new java.util.ArrayList<Integer>();
    for (int i = 0; i < cantidad; i++) {
      int residente = residenteNuevo();
      asignaciones.asignar(rotacion, residente);
      ids.add(residente);
    }
    return ids;
  }

  protected String estadoDe(RotacionId rotacion, int residenteId) {
    return jdbc.queryForObject(
      "select estado from rotacion_residente where id_residente=? and id_asignatura=? and fecha_inicio=? and fecha_fin=?",
      String.class,
      residenteId,
      rotacion.getAsignaturaId(),
      rotacion.getInicio(),
      rotacion.getFin()
    );
  }

  protected int cupoLibre(RotacionId rotacion) {
    return rotaciones.consultar(rotacion).cupoDisponible();
  }

  // ---------- Usuarios y sesión ----------

  protected Integer idRol(String nombreRol) {
    return jdbc.queryForObject(
      "select id_rol from rol where lower(nombre_rol)=lower(?)",
      Integer.class,
      nombreRol
    );
  }

  protected UsuarioRequest datosDeCoordinador(String correo, Integer servicioId) {
    return new UsuarioRequest(
      "Coordinador",
      "De Prueba",
      correo,
      idRol("Coordinador"),
      CONTRASENA_VALIDA,
      servicioId
    );
  }

  protected UsuarioResponse coordinadorDe(Integer servicioId) {
    return usuarios.crear(datosDeCoordinador(UUID.randomUUID() + "@example.test", servicioId));
  }

  /** A partir de aquí, la prueba actúa con la sesión de ese usuario. */
  protected void actuarComo(String correo) {
    var usuario = cargadorUsuario.loadUserByUsername(correo);
    SecurityContextHolder.getContext().setAuthentication(
      UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities())
    );
  }

  protected long totalMovimientos() {
    return jdbc.queryForObject("select count(*) from historial_auditoria", Long.class);
  }
}
