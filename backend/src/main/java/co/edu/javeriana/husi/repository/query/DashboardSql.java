package co.edu.javeriana.husi.repository.query;

/**
 * Consultas del dashboard. Cada «%s» recibe condiciones opcionales ya armadas (« and …»), con
 * sus valores siempre enlazados como parámetros. Solo se usa SQL que acepten PostgreSQL y H2
 * (perfil local): los cálculos por día se hacen en Java.
 */
public final class DashboardSql {

  private DashboardSql() {}

  /** Semestre AAAA-1 (enero a junio) o AAAA-2 (julio a diciembre) de una columna de fecha. */
  public static String periodoDe(String columna) {
    return (
      "extract(year from %1$s)::int::text || '-' || " +
      "case when extract(month from %1$s) <= 6 then '1' else '2' end"
    ).formatted(columna);
  }

  /** Base de los conteos por rotación; el «%s» recibe los filtros del corte. */
  public static final String DESDE_ROTACIONES =
    " from (" + ListadoSql.ROTACIONES + ") datos where estado <> 'Cancelado'%s";

  public static final String TOTALES = """
  select count(distinct ("asignaturaId", inicio, fin)) as rotaciones,
         count(*) filter (where estado = 'Asignado') as asignaciones,
         count(*) as total
  %s
  """;

  public static final String ROTACIONES_POR_SERVICIO = """
  select servicio as etiqueta, count(distinct ("asignaturaId", inicio, fin)) as valor
  %s
  group by servicio
  order by servicio
  """;

  public static final String RESIDENTES_POR_UNIVERSIDAD = """
  select universidad as etiqueta, count(distinct "residenteId") as valor
  %s
  group by universidad
  order by valor desc, universidad
  """;

  public static final String INICIOS_POR_MES = """
  select to_char(inicio, 'YYYY-MM') as etiqueta,
         count(distinct ("asignaturaId", inicio, fin)) as valor
  %s
  group by to_char(inicio, 'YYYY-MM')
  order by etiqueta
  """;

  /**
   * Residentes del corte por programa. El join (no left join) con las rotaciones del periodo
   * deja fuera a los residentes que no tienen ninguna en él. Recibe: rotación, residente.
   */
  public static final String SITUACION_POR_PROGRAMA = """
  with base as (
    select p.nombre_programa as programa, bool_or(rr.estado = 'Asignado') as asignado
    from residente r
    join programa p on p.id_programa = r.id_programa
    join universidad u on u.id_universidad = p.id_universidad
    join rotacion_residente rr on rr.id_residente = r.id_residente
      and rr.estado <> 'Cancelado'%s
    where r.estado = 'Activo'%s
    group by r.id_residente, p.nombre_programa
  )
  select programa as etiqueta,
         count(*) filter (where asignado) as "conRotacion",
         count(*) filter (where not asignado) as "enEspera"
  from base
  group by programa
  order by count(*) desc, programa
  """;

  /**
   * Rotaciones de cada residente del periodo en el corte (puede ser 0 con un servicio o una
   * asignatura). Recibe: rotación contada, periodo del residente, residente.
   */
  public static final String CARGA_POR_RESIDENTE = """
  select n as rotaciones, count(*) as residentes
  from (
    select r.id_residente, count(rr.id_residente) as n
    from residente r
    join programa p on p.id_programa = r.id_programa
    join universidad u on u.id_universidad = p.id_universidad
    left join (rotacion_residente rr
      join asignatura a on a.id_asignatura = rr.id_asignatura
      join servicio s on s.id_servicio = a.id_servicio)
      on rr.id_residente = r.id_residente and rr.estado <> 'Cancelado'%s
    where r.estado = 'Activo'
      and exists (select 1 from rotacion_residente x
                  where x.id_residente = r.id_residente and x.estado <> 'Cancelado'%s)%s
    group by r.id_residente
  ) carga
  group by n
  """;

  public static final String RANGO_ASIGNACIONES = """
  select min(fecha_inicio) as desde, max(fecha_fin) as hasta
  from rotacion_residente
  where estado = 'Asignado'
  """;

  /** Tramo de cada asignación dentro de la ventana [:desde, :hasta]. Recibe: servicio. */
  public static final String TRAMOS_OCUPACION = """
  select s.nombre as servicio, s.cupo_simultaneo as cupo,
         greatest(rr.fecha_inicio, cast(:desde as date)) as desde,
         least(rr.fecha_fin, cast(:hasta as date)) as hasta
  from rotacion_residente rr
  join asignatura a on a.id_asignatura = rr.id_asignatura
  join servicio s on s.id_servicio = a.id_servicio
  where rr.estado = 'Asignado' and s.estado = 'Activo'
    and rr.fecha_inicio <= cast(:hasta as date)
    and rr.fecha_fin >= cast(:desde as date)%s
  """;

  /**
   * Pares de rotaciones asignadas del mismo residente que comparten al menos un día (las
   * consecutivas no). Cada par aparece una vez gracias a la comparación de llaves. Recibe:
   * condiciones del par y del residente.
   */
  public static final String PARES_CRUZADOS = """
  select a.id_residente as "residenteId", res.nombre_completo as residente,
         p.nombre_programa as programa,
         aa.nombre_asignatura as asignatura1, sa.nombre as servicio1,
         ab.nombre_asignatura as asignatura2, sb.nombre as servicio2,
         greatest(a.fecha_inicio, b.fecha_inicio) as desde,
         least(a.fecha_fin, b.fecha_fin) as hasta
  from rotacion_residente a
  join rotacion_residente b on b.id_residente = a.id_residente
    and (a.id_asignatura, a.fecha_inicio, a.fecha_fin)
        < (b.id_asignatura, b.fecha_inicio, b.fecha_fin)
    and a.fecha_inicio <= b.fecha_fin and b.fecha_inicio <= a.fecha_fin
  join asignatura aa on aa.id_asignatura = a.id_asignatura
  join servicio sa on sa.id_servicio = aa.id_servicio
  join asignatura ab on ab.id_asignatura = b.id_asignatura
  join servicio sb on sb.id_servicio = ab.id_servicio
  join residente res on res.id_residente = a.id_residente
  join programa p on p.id_programa = res.id_programa
  join universidad u on u.id_universidad = p.id_universidad
  where a.estado = 'Asignado' and b.estado = 'Asignado' and res.estado = 'Activo'%s
  order by res.nombre_completo, a.id_residente,
           greatest(a.fecha_inicio, b.fecha_inicio), aa.nombre_asignatura, ab.nombre_asignatura
  """;

  /** Recibe: condiciones de la rotación y del residente. */
  public static final String RESIDENTES_EN_ESPERA = """
  select r.id_residente as "residenteId", r.nombre_completo as residente,
         p.nombre_programa as programa, 'espera' as motivo,
         string_agg(s.nombre || ' desde ' || to_char(rr.fecha_inicio, 'YYYY-MM-DD'), ', '
                    order by rr.fecha_inicio) as detalle
  from rotacion_residente rr
  join residente r on r.id_residente = rr.id_residente
  join programa p on p.id_programa = r.id_programa
  join universidad u on u.id_universidad = p.id_universidad
  join asignatura a on a.id_asignatura = rr.id_asignatura
  join servicio s on s.id_servicio = a.id_servicio
  where rr.estado = 'Esperando cupo' and r.estado = 'Activo'%s
  group by r.id_residente, r.nombre_completo, p.nombre_programa
  order by min(rr.fecha_inicio), r.nombre_completo
  """;

  public static final String SERVICIOS_SIN_COORDINADOR = """
  select count(*)
  from servicio s
  where s.estado = 'Activo'
    and not exists (select 1 from coordinador c
                    where c.id_servicio = s.id_servicio and c.estado = 'Activo')
  """;

  /**
   * En la base el programa y el servicio no se relacionan directamente: se vinculan cuando un
   * residente del programa rota en una asignatura del servicio.
   */
  public static final String RELACIONES_FILTROS = """
  select distinct p.nombre_programa as programa, s.nombre as servicio,
         a.id_asignatura as "asignaturaId"
  from rotacion_residente rr
  join residente r on r.id_residente = rr.id_residente
  join programa p on p.id_programa = r.id_programa
  join asignatura a on a.id_asignatura = rr.id_asignatura
  join servicio s on s.id_servicio = a.id_servicio
  where rr.estado <> 'Cancelado' and p.estado <> 'Eliminado'
    and s.estado <> 'Eliminado' and a.estado <> 'Eliminado'
  order by programa, servicio, "asignaturaId"
  """;
}
