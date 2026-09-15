package co.edu.javeriana.husi.repository;

import co.edu.javeriana.husi.dto.response.PaginaResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.hibernate.query.NativeQuery;
import java.util.*;

/** Proyecciones de lectura: SQL fijo, parámetros enlazados y paginación en PostgreSQL. */
@Repository @RequiredArgsConstructor
public class ConsultaRepository {
 private final EntityManager em;
 public static final String PERIODO = "extract(year from fecha_inicio)::int::text || '-' || case when extract(month from fecha_inicio)<=6 then '1' else '2' end";
 public static final String ROTACIONES = """
 select concat(rr.id_residente, '-', rr.id_asignatura, '-', rr.fecha_inicio, '-', rr.fecha_fin) as id,
 res.nombre_completo as residente, u.nombre_universidad as universidad, s.nombre as servicio,
 a.nombre_asignatura as asignatura, rr.fecha_inicio as inicio, rr.fecha_fin as fin, rr.estado,
 '—' as doctor, rr.id_residente as "residenteId", rr.id_asignatura as "asignaturaId"
 from rotacion_residente rr join residente res using(id_residente)
 join programa p on p.id_programa=res.id_programa join universidad u using(id_universidad)
 join asignatura a using(id_asignatura) join servicio s using(id_servicio)
 """;
 private String base(String dominio) {
  return switch(dominio) {
   case "programas" -> "select p.id_programa as id,p.nombre_programa as nombre,u.id_universidad as \"universidadId\",u.nombre_universidad as universidad,p.estado from programa p join universidad u using(id_universidad) where p.estado<>'Eliminado'";
   case "universidades" -> "select id_universidad as id,nombre_universidad as nombre,pais,perfil,estado from universidad where estado<>'Eliminado'";
   case "servicios" -> "select id_servicio as id,nombre,descripcion,cupo_simultaneo as \"cupoSimultaneo\",cupo_maximo_instalado as \"cupoMaximoInstalado\",estado='Activo' as activo,estado from servicio where estado<>'Eliminado'";
   case "usuarios" -> "select u.id_usuario as id,concat(u.nombre,' ',u.apellido) as nombre,u.correo,r.nombre_rol as rol,u.estado='Activo' as activo,u.estado from usuario u join rol r using(id_rol) where u.estado<>'Eliminado'";
   case "residentes" -> "select res.id_residente as id,res.nombre_completo as nombre,concat(res.tipo_documento,' ',res.numero_documento) as documento,u.nombre_universidad as universidad,p.nombre_programa as programa,upper(res.estado) as estado,coalesce((select string_agg(distinct " + PERIODO + ", ', ' order by " + PERIODO + ") from rotacion_residente where id_residente=res.id_residente and estado<>'Cancelado'),'—') as periodo from residente res join programa p using(id_programa) join universidad u using(id_universidad) where res.estado<>'Eliminado'";
   case "historial" -> "select h.id_historial as id,to_char(h.fecha at time zone 'America/Bogota','YYYY-MM-DD\"T\"HH24:MI:SS') || '-05:00' as fecha,concat(u.nombre,' ',u.apellido) as usuario,h.entidad_modificada as modulo,h.accion_realizada as accion,'—' as registro,h.detalle from historial_auditoria h join usuario u using(id_usuario)";
   case "rotaciones" -> ROTACIONES;
   default -> throw new IllegalArgumentException("Dominio no disponible");
  };
 }
 public PaginaResponse<Map<String,Object>> listar(String dominio, Map<String,String> filtros, int pagina, int tamano) {
  var parametros=new HashMap<String,Object>();
  StringBuilder where=new StringBuilder(" where 1=1");
  String campos=switch(dominio) {
   case "programas" -> "concat(nombre,' ',universidad)";
   case "residentes" -> "concat(nombre,' ',documento)";
   case "usuarios" -> "concat(nombre,' ',correo)";
   case "historial" -> "concat(usuario,' ',modulo,' ',accion,' ',detalle)";
   case "servicios" -> "concat(nombre,' ',descripcion)";
   case "universidades" -> "concat(nombre,' ',pais,' ',perfil)";
   case "rotaciones" -> "concat(residente,' ',universidad,' ',servicio,' ',asignatura)";
   default -> "nombre";
  };
  filtros.forEach((clave,valor)->{
   switch(clave) {
    case "universidadId" -> where.append(" and \"universidadId\"=:universidadId");
    case "q" -> where.append(" and strpos(lower("+campos+"),lower(:q))>0");
    case "desde" -> where.append(" and cast(fecha as timestamptz)>=cast(:desde as date) at time zone 'America/Bogota'");
    case "hasta" -> where.append(" and cast(fecha as timestamptz)<(cast(:hasta as date)+1) at time zone 'America/Bogota'");
    case "periodo" -> {
     if(dominio.equals("residentes")) where.append(" and :periodo=any(string_to_array(periodo, ', '))");
     else where.append(" and extract(year from inicio)::int::text || '-' || case when extract(month from inicio)<=6 then '1' else '2' end = :periodo");
    }
    case "proximas" -> where.append(" and inicio>(current_timestamp at time zone 'America/Bogota')::date and inicio<=(current_timestamp at time zone 'America/Bogota')::date+30 and estado<>'Cancelado'");
    default -> where.append(" and lower("+clave+")=lower(:"+clave+")");
   }
   if(!clave.equals("proximas")) parametros.put(clave,clave.equals("universidadId") ? Integer.valueOf(valor) : valor);
  });
  String consulta=" from ("+base(dominio)+") datos"+where;
  long total=((Number)escalar("select count(*)"+consulta,parametros)).longValue();
  int paginas=Math.max(1,(int)Math.ceil((double)total/tamano));
  int actual=Math.min(pagina,paginas);
  String orden=dominio.equals("historial") ? "cast(fecha as timestamptz) desc,id desc" : dominio.equals("rotaciones") ? "inicio,id" : "lower(nombre),id";
  var query=em.createNativeQuery("select *"+consulta+" order by "+orden);
  parametros.forEach(query::setParameter);
  query.setFirstResult((actual-1)*tamano).setMaxResults(tamano);
  return new PaginaResponse<>(mapas(query.unwrap(NativeQuery.class)),total,paginas,actual,tamano);
 }
 @SuppressWarnings({"unchecked","rawtypes"})
 private List<Map<String,Object>> mapas(NativeQuery query) {
  query.setTupleTransformer((tuple,aliases)->{
   Map<String,Object> row=new LinkedHashMap<>();
   for(int i=0;i<aliases.length;i++) row.put(aliases[i],tuple[i] instanceof java.sql.Date d ? d.toString() : tuple[i]);
   return row;
  });
  return query.getResultList();
 }
 public List<Map<String,Object>> consultar(String sql,Map<String,Object> params) {
  var query=em.createNativeQuery(sql); params.forEach(query::setParameter);
  return mapas(query.unwrap(NativeQuery.class));
 }
 public Object escalar(String sql,Map<String,Object> params) {
  var query=em.createNativeQuery(sql); params.forEach(query::setParameter); return query.getSingleResult();
 }
 public Map<String,List<String>> catalogos() {
  Map<String,List<String>> result=new LinkedHashMap<>();
  Map<String,String> sql=Map.of(
   "universidades","select nombre_universidad as nombre from universidad where estado<>'Eliminado' order by nombre",
   "programas","select distinct nombre_programa as nombre from programa where estado<>'Eliminado' order by nombre",
   "servicios","select nombre from servicio where estado<>'Eliminado' order by nombre",
   "roles","select nombre_rol as nombre from rol order by nombre",
   "modulos","select distinct entidad_modificada as nombre from historial_auditoria order by nombre",
   "acciones","select distinct accion_realizada as nombre from historial_auditoria order by nombre",
   "periodos","select distinct "+PERIODO+" as nombre from rotacion order by nombre desc");
  sql.forEach((key,value)->result.put(key,consultar(value,Map.of()).stream().map(row->row.get("nombre").toString()).toList()));
  return result;
 }
}
