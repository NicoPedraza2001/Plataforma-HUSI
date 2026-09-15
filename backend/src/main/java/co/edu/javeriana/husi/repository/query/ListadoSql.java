package co.edu.javeriana.husi.repository.query;

/** Proyecciones SQL internas por dominio; los valores de filtros siempre se enlazan. */
public final class ListadoSql {

  private ListadoSql() {}

  public static final String PERIODO =
    "extract(year from fecha_inicio)::int::text || '-' || case when extract(month from fecha_inicio)<=6 then '1' else '2' end";
  public static final String ROTACIONES = """
  select concat(rr.id_residente, '-', rr.id_asignatura, '-', rr.fecha_inicio, '-', rr.fecha_fin) as id,
  res.nombre_completo as residente, u.nombre_universidad as universidad, s.nombre as servicio,
  a.nombre_asignatura as asignatura, rr.fecha_inicio as inicio, rr.fecha_fin as fin, rr.estado,
  '—' as doctor, rr.id_residente as "residenteId", rr.id_asignatura as "asignaturaId"
  from rotacion_residente rr join residente res using(id_residente)
  join programa p on p.id_programa=res.id_programa join universidad u using(id_universidad)
  join asignatura a using(id_asignatura) join servicio s using(id_servicio)
  """;

  public static String base(String dominio) {
    return switch (dominio) {
      case "programas" -> "select p.id_programa as id,p.nombre_programa as nombre,u.id_universidad as \"universidadId\",u.nombre_universidad as universidad,p.estado from programa p join universidad u using(id_universidad) where p.estado<>'Eliminado'";
      case "universidades" -> "select id_universidad as id,nombre_universidad as nombre,pais,perfil,estado from universidad where estado<>'Eliminado'";
      case "servicios" -> "select id_servicio as id,nombre,descripcion,cupo_simultaneo as \"cupoSimultaneo\",cupo_maximo_instalado as \"cupoMaximoInstalado\",estado='Activo' as activo,estado from servicio where estado<>'Eliminado'";
      case "usuarios" -> "select u.id_usuario as id,concat(u.nombre,' ',u.apellido) as nombre,u.correo,r.nombre_rol as rol,u.estado='Activo' as activo,u.estado from usuario u join rol r using(id_rol) where u.estado<>'Eliminado'";
      case "residentes" -> "select res.id_residente as id,res.nombre_completo as nombre,concat(res.tipo_documento,' ',res.numero_documento) as documento,u.nombre_universidad as universidad,p.nombre_programa as programa,upper(res.estado) as estado,coalesce((select string_agg(distinct " +
        PERIODO +
        ", ', ' order by " +
        PERIODO +
        ") from rotacion_residente where id_residente=res.id_residente and estado<>'Cancelado'),'—') as periodo from residente res join programa p using(id_programa) join universidad u using(id_universidad) where res.estado<>'Eliminado'";
      case "historial" -> "select h.id_historial as id,to_char(h.fecha at time zone 'America/Bogota','YYYY-MM-DD\"T\"HH24:MI:SS') || '-05:00' as fecha,concat(u.nombre,' ',u.apellido) as usuario,h.entidad_modificada as modulo,h.accion_realizada as accion,'—' as registro,h.detalle from historial_auditoria h join usuario u using(id_usuario)";
      case "rotaciones" -> ROTACIONES;
      default -> throw new IllegalArgumentException("Dominio no disponible");
    };
  }
}
