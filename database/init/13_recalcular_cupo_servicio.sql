-- ============================================================
-- Cambiar servicio.cupo_simultaneo recalcula el cupo de todas sus
-- rotaciones: cupo_disponible = cupo_simultaneo - residentes 'Asignado'.
-- No se permite bajar el cupo por debajo de los asignados de ninguna
-- rotacion. Si sube, se promueve la lista de espera por orden de llegada.
-- Las rotaciones finalizadas (fecha_fin anterior a hoy) son de solo
-- lectura: no se recalculan ni cuentan para el minimo.
-- ============================================================

CREATE OR REPLACE FUNCTION trg_fn_servicio_recalcular_cupo()
RETURNS TRIGGER
AS $$
DECLARE
    v_minimo INTEGER;
BEGIN
    -- Bloquea las rotaciones para que ninguna asignacion concurrente
    -- cambie el conteo entre la validacion y el recalculo.
    PERFORM 1
    FROM   rotacion   r
    JOIN   asignatura a ON a.id_asignatura = r.id_asignatura
    WHERE  a.id_servicio = NEW.id_servicio
      AND  r.fecha_fin >= (current_timestamp AT TIME ZONE 'America/Bogota')::date
    FOR UPDATE OF r;

    SELECT COALESCE(MAX(t.asignados), 0)
    INTO   v_minimo
    FROM  (SELECT COUNT(*) AS asignados
           FROM   rotacion_residente rr
           JOIN   asignatura a ON a.id_asignatura = rr.id_asignatura
           WHERE  a.id_servicio = NEW.id_servicio
             AND  rr.estado = 'Asignado'
             AND  rr.fecha_fin >= (current_timestamp AT TIME ZONE 'America/Bogota')::date
           GROUP BY rr.id_asignatura, rr.fecha_inicio, rr.fecha_fin) t;

    IF NEW.cupo_simultaneo < v_minimo THEN
        RAISE EXCEPTION
            'El cupo simultaneo del servicio % no puede ser menor a %: una de sus rotaciones vigentes ya tiene % residentes asignados.',
            NEW.id_servicio, v_minimo, v_minimo
            USING ERRCODE = 'check_violation';
    END IF;

    -- pg_trigger_depth() > 1: trg_rotacion_proteger_cupo lo permite.
    UPDATE rotacion r
    SET    cupo_disponible = NEW.cupo_simultaneo - c.asignados,
           estado = CASE
                        WHEN NEW.cupo_simultaneo - c.asignados = 0 THEN 'Cupo Lleno'
                        ELSE 'Disponible'
                    END
    FROM  (SELECT r2.id_asignatura, r2.fecha_inicio, r2.fecha_fin,
                  COUNT(rr.id_residente) FILTER (WHERE rr.estado = 'Asignado') AS asignados
           FROM   rotacion   r2
           JOIN   asignatura a ON a.id_asignatura = r2.id_asignatura
           LEFT JOIN rotacion_residente rr
                  ON rr.id_asignatura = r2.id_asignatura
                 AND rr.fecha_inicio  = r2.fecha_inicio
                 AND rr.fecha_fin     = r2.fecha_fin
           WHERE  a.id_servicio = NEW.id_servicio
             AND  r2.fecha_fin >= (current_timestamp AT TIME ZONE 'America/Bogota')::date
           GROUP BY r2.id_asignatura, r2.fecha_inicio, r2.fecha_fin) c
    WHERE  r.id_asignatura = c.id_asignatura
      AND  r.fecha_inicio  = c.fecha_inicio
      AND  r.fecha_fin     = c.fecha_fin;

    -- Si subio el cupo, entra la lista de espera (14_lista_espera.sql).
    IF NEW.cupo_simultaneo > OLD.cupo_simultaneo THEN
        PERFORM promover_espera_rotacion(r.id_asignatura, r.fecha_inicio, r.fecha_fin)
        FROM   rotacion   r
        JOIN   asignatura a ON a.id_asignatura = r.id_asignatura
        WHERE  a.id_servicio = NEW.id_servicio;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_servicio_recalcular_cupo ON servicio;
CREATE TRIGGER trg_servicio_recalcular_cupo
    AFTER UPDATE OF cupo_simultaneo ON servicio
    FOR EACH ROW
    WHEN (OLD.cupo_simultaneo IS DISTINCT FROM NEW.cupo_simultaneo)
    EXECUTE FUNCTION trg_fn_servicio_recalcular_cupo();
