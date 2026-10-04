-- ============================================================
-- Funciones de cupo, auditoria y consulta
-- Se ejecuta automaticamente al levantar el contenedor 
-- ============================================================

CREATE OR REPLACE FUNCTION inicializar_cupo_rotacion()
RETURNS TRIGGER
AS $$
DECLARE
    v_cupo             INTEGER;
    v_estado_servicio  VARCHAR(20);
    v_estado_asig      VARCHAR(20);
BEGIN
    SELECT s.cupo_simultaneo, s.estado, a.estado
    INTO   v_cupo, v_estado_servicio, v_estado_asig
    FROM   asignatura a
    JOIN   servicio   s ON s.id_servicio = a.id_servicio
    WHERE  a.id_asignatura = NEW.id_asignatura;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'No existe la asignatura % al crear la rotacion.', NEW.id_asignatura
            USING ERRCODE = 'foreign_key_violation';
    END IF;

    IF v_estado_asig <> 'Activo' THEN
        RAISE EXCEPTION
            'La asignatura % no esta activa; no se pueden abrir rotaciones.',
            NEW.id_asignatura
            USING ERRCODE = 'check_violation';
    END IF;

    IF v_estado_servicio <> 'Activo' THEN
        RAISE EXCEPTION
            'El servicio de la asignatura % no esta activo.', NEW.id_asignatura
            USING ERRCODE = 'check_violation';
    END IF;

    -- La rotacion nace con el cupo simultaneo del servicio. Si despues
    -- cambia servicio.cupo_simultaneo, trg_servicio_recalcular_cupo
    -- (13_recalcular_cupo_servicio.sql) recalcula las rotaciones abiertas.
    NEW.cupo_disponible := v_cupo;

    IF v_cupo = 0 THEN
        NEW.estado := 'Cupo Lleno';
    ELSE
        NEW.estado := 'Disponible';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION consumir_cupo_rotacion(
    p_id_asignatura  INTEGER,
    p_fecha_inicio   DATE,
    p_fecha_fin      DATE
)
RETURNS BOOLEAN
AS $$
DECLARE
    v_cupo INTEGER;
BEGIN
    SELECT r.cupo_disponible
    INTO   v_cupo
    FROM   rotacion r
    WHERE  r.id_asignatura = p_id_asignatura
      AND  r.fecha_inicio  = p_fecha_inicio
      AND  r.fecha_fin     = p_fecha_fin
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'No existe la rotacion (asignatura %, % a %).',
            p_id_asignatura, p_fecha_inicio, p_fecha_fin
            USING ERRCODE = 'foreign_key_violation';
    END IF;

    IF v_cupo <= 0 THEN
        RETURN FALSE;
    END IF;

    UPDATE rotacion
    SET cupo_disponible = cupo_disponible - 1,
        estado = CASE
                     WHEN cupo_disponible - 1 = 0 THEN 'Cupo Lleno'
                     ELSE 'Disponible'
                 END
    WHERE id_asignatura = p_id_asignatura
      AND fecha_inicio  = p_fecha_inicio
      AND fecha_fin     = p_fecha_fin;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION liberar_cupo_rotacion(
    p_id_asignatura  INTEGER,
    p_fecha_inicio   DATE,
    p_fecha_fin      DATE
)
RETURNS VOID
AS $$
DECLARE
    v_cupo   INTEGER;
    v_techo  INTEGER;
BEGIN
    SELECT r.cupo_disponible, s.cupo_simultaneo
    INTO   v_cupo, v_techo
    FROM   rotacion   r
    JOIN   asignatura a ON a.id_asignatura = r.id_asignatura
    JOIN   servicio   s ON s.id_servicio   = a.id_servicio
    WHERE  r.id_asignatura = p_id_asignatura
      AND  r.fecha_inicio  = p_fecha_inicio
      AND  r.fecha_fin     = p_fecha_fin
    FOR UPDATE OF r;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'No existe la rotacion (asignatura %, % a %).',
            p_id_asignatura, p_fecha_inicio, p_fecha_fin
            USING ERRCODE = 'foreign_key_violation';
    END IF;

    IF v_cupo >= v_techo THEN
        RAISE EXCEPTION
            'Inconsistencia: se intento liberar un cupo en la rotacion (asignatura %, % a %) que ya esta en capacidad maxima (%).',
            p_id_asignatura, p_fecha_inicio, p_fecha_fin, v_techo
            USING ERRCODE = 'check_violation';
    END IF;

    UPDATE rotacion
    SET cupo_disponible = cupo_disponible + 1,
        estado          = 'Disponible'
    WHERE id_asignatura = p_id_asignatura
      AND fecha_inicio  = p_fecha_inicio
      AND fecha_fin     = p_fecha_fin;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trg_fn_rr_insert()
RETURNS TRIGGER
AS $$
DECLARE
    v_estado_res VARCHAR(20);
BEGIN
    SELECT estado INTO v_estado_res
    FROM   residente
    WHERE  id_residente = NEW.id_residente;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe el residente %.', NEW.id_residente
            USING ERRCODE = 'foreign_key_violation';
    END IF;

    IF v_estado_res <> 'Activo' THEN
        RAISE EXCEPTION
            'El residente % no esta activo; no puede recibir rotaciones.',
            NEW.id_residente
            USING ERRCODE = 'check_violation';
    END IF;

    -- Una fila insertada ya como 'Cancelado' no consume capacidad.
    IF NEW.estado = 'Cancelado' THEN
        RETURN NEW;
    END IF;

    IF consumir_cupo_rotacion(NEW.id_asignatura,
                              NEW.fecha_inicio,
                              NEW.fecha_fin) THEN
        NEW.estado := 'Asignado';
    ELSE
        NEW.estado := 'Esperando cupo';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trg_fn_rr_update()
RETURNS TRIGGER
AS $$
DECLARE
    v_cambio_rotacion BOOLEAN;
BEGIN
    v_cambio_rotacion := (OLD.id_asignatura, OLD.fecha_inicio, OLD.fecha_fin)
                      IS DISTINCT FROM
                         (NEW.id_asignatura, NEW.fecha_inicio, NEW.fecha_fin);

    -- 1. Suelta el cupo anterior si lo tenia.
    IF OLD.estado = 'Asignado'
       AND (v_cambio_rotacion OR NEW.estado <> 'Asignado')
    THEN
        PERFORM liberar_cupo_rotacion(
            OLD.id_asignatura, OLD.fecha_inicio, OLD.fecha_fin);
    END IF;

    -- 2. Toma el cupo nuevo si ahora lo necesita.
    IF NEW.estado = 'Asignado'
       AND (v_cambio_rotacion OR OLD.estado <> 'Asignado')
    THEN
        IF NOT consumir_cupo_rotacion(
                   NEW.id_asignatura, NEW.fecha_inicio, NEW.fecha_fin)
        THEN
            RAISE EXCEPTION
                'No hay cupo disponible en la rotacion (asignatura %, % a %) para el residente %.',
                NEW.id_asignatura, NEW.fecha_inicio, NEW.fecha_fin,
                NEW.id_residente
                USING ERRCODE = 'check_violation';
        END IF;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trg_fn_rr_delete()
RETURNS TRIGGER
AS $$
BEGIN
    IF OLD.estado = 'Asignado' THEN
        PERFORM liberar_cupo_rotacion(
            OLD.id_asignatura, OLD.fecha_inicio, OLD.fecha_fin);
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trg_fn_rotacion_proteger_cupo()
RETURNS TRIGGER
AS $$
BEGIN
    IF pg_trigger_depth() = 1 THEN
        RAISE EXCEPTION
            'cupo_disponible no se modifica directamente. Se ajusta creando, cancelando o eliminando filas en rotacion_residente.'
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- El backend declara el usuario de aplicacion al abrir la transaccion:
--     SET LOCAL app.id_usuario = '4';

CREATE OR REPLACE FUNCTION usuario_actual()
RETURNS INTEGER
AS $$
BEGIN
    RETURN NULLIF(current_setting('app.id_usuario', true), '')::INTEGER;
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION registrar_auditoria(
    p_accion   VARCHAR(100),
    p_entidad  VARCHAR(100),
    p_detalle  TEXT DEFAULT NULL
)
RETURNS VOID
AS $$
DECLARE
    v_id_usuario INTEGER;
BEGIN
    v_id_usuario := usuario_actual();

    IF v_id_usuario IS NULL THEN
        RAISE EXCEPTION
            'No se configuro app.id_usuario en la transaccion; no se puede auditar la accion %.',
            p_accion
            USING ERRCODE = 'null_value_not_allowed';
    END IF;

    INSERT INTO historial_auditoria (
        id_usuario, accion_realizada, entidad_modificada, detalle)
    VALUES (
        v_id_usuario, p_accion, p_entidad, p_detalle);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION residentes_simultaneos(
    p_id_servicio  INTEGER,
    p_fecha        DATE
)
RETURNS INTEGER
AS $$
DECLARE
    v_total INTEGER;
BEGIN
    SELECT COUNT(DISTINCT rr.id_residente)
    INTO   v_total
    FROM   rotacion_residente rr
    JOIN   asignatura a ON a.id_asignatura = rr.id_asignatura
    WHERE  a.id_servicio  = p_id_servicio
      AND  rr.estado      = 'Asignado'
      AND  p_fecha BETWEEN rr.fecha_inicio AND rr.fecha_fin;

    RETURN v_total;
END;
$$ LANGUAGE plpgsql STABLE;

