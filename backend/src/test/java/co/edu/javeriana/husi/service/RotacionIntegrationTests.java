package co.edu.javeriana.husi.service;
import co.edu.javeriana.husi.dto.request.*;
import co.edu.javeriana.husi.model.entity.RotacionId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"spring.jpa.show-sql=false"}) @Transactional
class RotacionIntegrationTests {
 @Autowired RotacionService servicio;
 @Autowired ImportacionRotacionesService importacion;
 @Autowired JdbcTemplate jdbc;
 private Integer asignatura(){
  Integer s=jdbc.queryForObject("insert into servicio(nombre,cupo_maximo_instalado,cupo_simultaneo) values(?,3,3) returning id_servicio",Integer.class,"TEST-ROT-S-"+UUID.randomUUID());
  return jdbc.queryForObject("insert into asignatura(id_servicio,nombre_asignatura) values(?,?) returning id_asignatura",Integer.class,s,"TEST-ROT-A-"+UUID.randomUUID());
 }
 private RotacionRequest datos(int id){return new RotacionRequest(id,LocalDate.of(2091,1,1),LocalDate.of(2091,1,31));}
 private RotacionId clave(RotacionRequest d){return new RotacionId(d.asignaturaId(),d.inicio(),d.fin());}
 private Integer residente(){
  int u=jdbc.queryForObject("insert into universidad(nombre_universidad,pais) values(?,'Colombia') returning id_universidad",Integer.class,"TEST-ROT-U-"+UUID.randomUUID());
  int p=jdbc.queryForObject("insert into programa(id_universidad,nombre_programa) values(?,'TEST ROT') returning id_programa",Integer.class,u);
  return jdbc.queryForObject("insert into residente(id_programa,nombre_completo,numero_documento) values(?,'Residente ficticio',?) returning id_residente",Integer.class,p,UUID.randomUUID().toString());
 }
 @Test void crearEditarEstadoYEliminarPersistenSinPerderCupo(){
  var d=datos(asignatura());var r=servicio.crear(d);assertThat(r.get("cupoDisponible")).isEqualTo(3);
  assertThat(servicio.estado(clave(d),false).get("activa")).isEqualTo(false);
  assertThat(jdbc.queryForObject("select count(*) from rotacion where id_asignatura=?",Integer.class,d.asignaturaId())).isEqualTo(1);
  assertThat(servicio.estado(clave(d),true).get("activa")).isEqualTo(true);
  var editado=new RotacionRequest(d.asignaturaId(),d.inicio().plusDays(1),d.fin());
  assertThat(servicio.editar(clave(d),editado).get("inicio").toString()).isEqualTo("2091-01-02");
  assertThat(servicio.consultar(clave(editado)).get("cupoDisponible")).isEqualTo(3);
  servicio.eliminar(clave(editado));
  assertThat(jdbc.queryForObject("select count(*) from rotacion where id_asignatura=?",Integer.class,d.asignaturaId())).isZero();
 }
 @Test void protegeRelacionesInclusoCanceladas(){
  var d=datos(asignatura());servicio.crear(d);
  jdbc.update("insert into rotacion_residente(id_residente,id_asignatura,fecha_inicio,fecha_fin,estado) values(?,?,?,?,'Cancelado')",residente(),d.asignaturaId(),d.inicio(),d.fin());
  assertThatThrownBy(()->servicio.eliminar(clave(d))).hasMessageContaining("relacionados");
  assertThatThrownBy(()->servicio.editar(clave(d),new RotacionRequest(d.asignaturaId(),d.inicio().plusDays(1),d.fin()))).hasMessageContaining("historial");
  assertThat(servicio.estado(clave(d),false).get("relaciones")).isEqualTo(1L);
 }
 @Test void inactivaBloqueaNuevasAsignacionesEnLaBase(){
  var d=datos(asignatura());servicio.crear(d);int residente=residente();servicio.estado(clave(d),false);
  assertThatThrownBy(()->jdbc.update("insert into rotacion_residente(id_residente,id_asignatura,fecha_inicio,fecha_fin) values(?,?,?,?)",residente,d.asignaturaId(),d.inicio(),d.fin())).hasMessageContaining("inactiva");
 }
 @Test void cancelarAsignacionNoReactivaRotacion(){
  var d=datos(asignatura());servicio.crear(d);int residente=residente();
  jdbc.update("insert into rotacion_residente(id_residente,id_asignatura,fecha_inicio,fecha_fin) values(?,?,?,?)",residente,d.asignaturaId(),d.inicio(),d.fin());
  servicio.estado(clave(d),false);
  jdbc.update("update rotacion_residente set estado='Cancelado' where id_residente=?",residente);
  var r=servicio.consultar(clave(d));assertThat(r.get("activa")).isEqualTo(false);assertThat(r.get("cupoDisponible")).isEqualTo(3);
 }
 @Test void busquedaFiltrosYPaginacionConsultanBase(){
  var d=datos(asignatura());servicio.crear(d);servicio.crear(new RotacionRequest(d.asignaturaId(),d.inicio().plusYears(1),d.fin().plusYears(1)));
  String nombre=servicio.consultar(clave(d)).get("asignatura").toString();
  var lista=servicio.listar(Map.of("q",nombre,"tamano","1","pagina","2"));assertThat(lista.totalElementos()).isEqualTo(2);assertThat(lista.pagina()).isEqualTo(2);
  servicio.estado(clave(d),false);assertThat(servicio.listar(Map.of("q",nombre,"activa","false")).totalElementos()).isEqualTo(1);
  assertThat(servicio.listar(Map.of("q","no-existe-"+UUID.randomUUID())).totalElementos()).isZero();
  assertThatThrownBy(()->servicio.listar(Map.of("pagina","0"))).hasMessageContaining("válido");
 }
 @Test void validaFechasDuplicadosYAsignatura(){
  var d=datos(asignatura());servicio.crear(d);
  assertThatThrownBy(()->servicio.crear(d)).hasMessageContaining("ya existe");
  assertThatThrownBy(()->servicio.crear(new RotacionRequest(d.asignaturaId(),d.fin(),d.inicio()))).hasMessageContaining("anterior");
  assertThatThrownBy(()->servicio.crear(datos(Integer.MAX_VALUE))).hasMessageContaining("no existe");
  assertThatThrownBy(()->servicio.crear(new RotacionRequest(null,null,null))).hasMessageContaining("obligatorias");
 }
 @Test void importacionValidaSinGuardarOmiteDuplicadosYRegistraSoloValidas(){
  int id=asignatura();String a=jdbc.queryForObject("select nombre_asignatura from asignatura where id_asignatura=?",String.class,id);
  String s=jdbc.queryForObject("select s.nombre from servicio s join asignatura a using(id_servicio) where a.id_asignatura=?",String.class,id);
  var req=new ImportacionRotacionesRequest(List.of(new ImportacionRotacionesRequest.Fila(2,a,s,"2092-01-01","2092-01-31"),new ImportacionRotacionesRequest.Fila(3,a,s,"2092-01-01","2092-01-31"),new ImportacionRotacionesRequest.Fila(4,a,s,"mal","2092-01-31")));
  var validar=importacion.procesar(req,false);assertThat(validar.get("VALIDO")).isEqualTo(1L);assertThat(validar.get("OMITIDO")).isEqualTo(1L);assertThat(validar.get("ERROR")).isEqualTo(1L);
  assertThat(jdbc.queryForObject("select count(*) from rotacion where id_asignatura=?",Integer.class,id)).isZero();
  assertThat(importacion.procesar(req,true).get("CREADO")).isEqualTo(1L);
  assertThat(importacion.procesar(req,true).get("CREADO")).isEqualTo(0L);
 }
}
