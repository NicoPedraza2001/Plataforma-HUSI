package co.edu.javeriana.husi.casosdeuso;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.javeriana.husi.InsercionDePrueba;
import co.edu.javeriana.husi.model.entity.RotacionId;
import co.edu.javeriana.husi.security.CargadorUsuarioAutenticado;
import co.edu.javeriana.husi.service.RotacionResidenteService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * CU07-EX10: dos asignaciones simultáneas por el último cupo. No es transaccional porque cada hilo
 * debe confirmar su propia transacción; por eso borra al final los datos que creó.
 */
@SpringBootTest
class CU07ConcurrenciaIntegrationTests {

  private static final LocalDate INICIO = LocalDate.of(2092, 1, 1);
  private static final LocalDate FIN = LocalDate.of(2092, 1, 31);

  @Autowired
  RotacionResidenteService asignaciones;

  @Autowired
  CargadorUsuarioAutenticado cargadorUsuario;

  @Autowired
  JdbcTemplate jdbc;

  private final String marca = UUID.randomUUID().toString();
  private final List<Integer> residentesCreados = new ArrayList<>();
  private Integer servicioId;
  private Integer asignaturaId;
  private Integer programaId;
  private Integer universidadId;
  private Long ultimoMovimientoAntesDeLaPrueba;

  private RotacionId prepararRotacionConUnCupo() {
    ultimoMovimientoAntesDeLaPrueba = jdbc.queryForObject(
      "select coalesce(max(id_historial),0) from historial_auditoria",
      Long.class
    );
    servicioId = InsercionDePrueba.insertarYObtenerId(
      jdbc,
      "id_servicio",
      "insert into servicio(nombre,cupo_maximo_instalado,cupo_simultaneo) values(?,1,1)",
      "Servicio " + marca
    );
    asignaturaId = InsercionDePrueba.insertarYObtenerId(
      jdbc,
      "id_asignatura",
      "insert into asignatura(id_servicio,nombre_asignatura) values(?,?)",
      servicioId,
      "Asignatura " + marca
    );
    jdbc.update(
      "insert into rotacion(id_asignatura,fecha_inicio,fecha_fin) values (?,?,?)",
      asignaturaId,
      INICIO,
      FIN
    );
    universidadId = InsercionDePrueba.insertarYObtenerId(
      jdbc,
      "id_universidad",
      "insert into universidad(nombre_universidad,pais) values(?,'Colombia')",
      "Universidad " + marca
    );
    programaId = InsercionDePrueba.insertarYObtenerId(
      jdbc,
      "id_programa",
      "insert into programa(id_universidad,nombre_programa) values(?,?)",
      universidadId,
      "Programa " + marca
    );
    for (int i = 0; i < 2; i++) residentesCreados.add(
      InsercionDePrueba.insertarYObtenerId(
        jdbc,
        "id_residente",
        "insert into residente(id_programa,id_tipo_documento,nombre_completo,numero_documento) values(?,1,?,?)",
        programaId,
        "Residente " + marca,
        UUID.randomUUID().toString()
      )
    );
    return new RotacionId(asignaturaId, INICIO, FIN);
  }

  private void iniciarSesionDeAdministradorEnEsteHilo() {
    var administrador = cargadorUsuario.loadUserByUsername("admin@husi.com");
    SecurityContextHolder.getContext().setAuthentication(
      UsernamePasswordAuthenticationToken.authenticated(
        administrador,
        null,
        administrador.getAuthorities()
      )
    );
  }

  @AfterEach
  void borrarDatosCreados() {
    if (ultimoMovimientoAntesDeLaPrueba == null) return;
    jdbc.update(
      "delete from historial_auditoria where id_historial>? and datos_nuevos like ?",
      ultimoMovimientoAntesDeLaPrueba,
      "%" + marca + "%"
    );
    jdbc.update("delete from rotacion_residente where id_asignatura=?", asignaturaId);
    jdbc.update("delete from rotacion where id_asignatura=?", asignaturaId);
    jdbc.update("delete from asignatura where id_asignatura=?", asignaturaId);
    jdbc.update("delete from servicio where id_servicio=?", servicioId);
    for (Integer residente : residentesCreados) jdbc.update(
      "delete from residente where id_residente=?",
      residente
    );
    jdbc.update("delete from programa where id_programa=?", programaId);
    jdbc.update("delete from universidad where id_universidad=?", universidadId);
  }

  @Test
  @DisplayName("CU07-EX10 Dos asignaciones simultáneas por el último cupo dejan una en espera")
  void dosAsignacionesSimultaneasDejanUnaEnEspera() throws Exception {
    RotacionId rotacion = prepararRotacionConUnCupo();
    var largada = new CountDownLatch(1);
    var hilos = Executors.newFixedThreadPool(2);
    try {
      List<Future<?>> asignacionesEnCurso = new ArrayList<>();
      for (Integer residente : residentesCreados) asignacionesEnCurso.add(
        hilos.submit(() -> {
          iniciarSesionDeAdministradorEnEsteHilo();
          largada.await();
          asignaciones.asignar(rotacion, residente);
          return null;
        })
      );
      largada.countDown();
      for (Future<?> asignacion : asignacionesEnCurso) asignacion.get();
    } finally {
      hilos.shutdownNow();
    }

    assertThat(
      jdbc.queryForList(
        "select estado from rotacion_residente where id_asignatura=?",
        String.class,
        asignaturaId
      )
    ).containsExactlyInAnyOrder("Asignado", "Esperando cupo");
    assertThat(
      jdbc.queryForObject(
        "select cupo_disponible from rotacion where id_asignatura=?",
        Integer.class,
        asignaturaId
      )
    ).isZero();
  }
}
