BEGIN;

-- ============================================================
-- Lista de espera por orden de llegada.
-- Cada vez que una rotacion gana cupo (se libera un cupo, sube el cupo
-- simultaneo del servicio o se reactiva la rotacion), los residentes en
-- 'Esperando cupo' pasan a 'Asignado' en orden de fecha_registro.
-- No se promueve en rotaciones terminadas, inactivas o cuya asignatura o
-- servicio esten inactivos, ni a residentes inactivos.
-- ============================================================

ALTER TABLE rotacion_residente
    ADD COLUMN IF NOT EXISTS fecha_registro TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp();

COMMENT ON COLUMN rotacion_residente.fecha_registro IS
    'Momento en que el residente se relaciono con la rotacion; ordena la lista de espera.';

-- Las firmas llevan un residente a excluir (opcional): quien acaba de perder su
-- cupo no lo recupera en el mismo momento. Se borran las versiones de 3
-- argumentos para que las llamadas existentes usen el valor por defecto.
DROP FUNCTION IF EXISTS promover_espera_rotacion(INTEGER, DATE, DATE);
DROP FUNCTION IF EXISTS liberar_cupo_rotacion(INTEGER, DATE, DATE);

CREATE INDEX IF NOT EXISTS idx_rr_espera_orden
    ON rotacion_residente (id_asignatura, fecha_inicio, fecha_fin, fecha_registro)
    WHERE estado = 'Esperando cupo';

CREATE OR REPLACE FUNCTION promover_espera_rotacion(
    p_id_asignatura  INTEGER,
    p_fecha_inicio   DATE,
    p_fecha_fin      DATE,
    p_excluir        INTEGER DEFAULT NULL
)
RETURNS INTEGER
AS $$
DECLARE
    v_cupo       INTEGER;
    v_residente  INTEGER;
    v_promovidos INTEGER := 0;
BEGIN
    SELECT r.cupo_disponible
    INTO   v_cupo
    FROM   rotacion   r
    JOIN   asignatura a ON a.id_asignatura = r.id_asignatura
    JOIN   servicio   s ON s.id_servicio   = a.id_servicio
    WHERE  r.id_asignatura = p_id_asignatura
      AND  r.fecha_inicio  = p_fecha_inicio
      AND  r.fecha_fin     = p_fecha_fin
      AND  r.activa
      AND  a.estado = 'Activo'
      AND  s.estado = 'Activo'
      AND  r.fecha_fin >= (current_timestamp AT TIME ZONE 'America/Bogota')::date
    FOR UPDATE OF r;

    IF NOT FOUND OR v_cupo <= 0 THEN
        RETURN 0;
    END IF;

    FOR v_residente IN
        SELECT rr.id_residente
        FROM   rotacion_residente rr
        JOIN   residente res ON res.id_residente = rr.id_residente
        WHERE  rr.id_asignatura = p_id_asignatura
          AND  rr.fecha_inicio  = p_fecha_inicio
          AND  rr.fecha_fin     = p_fecha_fin
          AND  rr.estado = 'Esperando cupo'
          AND  res.estado = 'Activo'
          AND  rr.id_residente IS DISTINCT FROM p_excluir
        ORDER BY rr.fecha_registro, rr.id_residente
        LIMIT v_cupo
    LOOP
        -- trg_rr_update descuenta el cupo de cada promovido.
        UPDATE rotacion_residente
        SET    estado = 'Asignado'
        WHERE  id_residente  = v_residente
          AND  id_asignatura = p_id_asignatura
          AND  fecha_inicio  = p_fecha_inicio
          AND  fecha_fin     = p_fecha_fin;
        v_promovidos := v_promovidos + 1;
    END LOOP;

    RETURN v_promovidos;
END;
$$ LANGUAGE plpgsql;

-- Reemplaza la version de 12_liberar_cupo_congelado.sql: ademas de liberar,
-- entrega el cupo al siguiente en espera.
CREATE OR REPLACE FUNCTION liberar_cupo_rotacion(
    p_id_asignatura  INTEGER,
    p_fecha_inicio   DATE,
    p_fecha_fin      DATE,
    p_excluir        INTEGER DEFAULT NULL
)
RETURNS VOID
AS $$
BEGIN
    PERFORM 1 FROM rotacion
    WHERE id_asignatura = p_id_asignatura AND fecha_inicio = p_fecha_inicio AND fecha_fin = p_fecha_fin
    FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe la rotacion (asignatura %, % a %).',
            p_id_asignatura, p_fecha_inicio, p_fecha_fin
            USING ERRCODE = 'foreign_key_violation';
    END IF;

    UPDATE rotacion
    SET    cupo_disponible = cupo_disponible + 1,
           estado          = 'Disponible'
    WHERE  id_asignatura = p_id_asignatura
      AND  fecha_inicio  = p_fecha_inicio
      AND  fecha_fin     = p_fecha_fin;

    PERFORM promover_espera_rotacion(p_id_asignatura, p_fecha_inicio, p_fecha_fin, p_excluir);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trg_fn_rotacion_reactivada()
RETURNS TRIGGER
AS $$
BEGIN
    PERFORM promover_espera_rotacion(NEW.id_asignatura, NEW.fecha_inicio, NEW.fecha_fin);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_rotacion_reactivada ON rotacion;
CREATE TRIGGER trg_rotacion_reactivada
    AFTER UPDATE OF activa ON rotacion
    FOR EACH ROW
    WHEN (NOT OLD.activa AND NEW.activa)
    EXECUTE FUNCTION trg_fn_rotacion_reactivada();

-- Reemplaza la version de 02_funciones.sql: quitar el cupo a un asignado lo
-- manda al final de la lista de espera (Asignado -> Esperando cupo) y su cupo
-- pasa al siguiente en espera, nunca de vuelta a el mismo.
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
            OLD.id_asignatura, OLD.fecha_inicio, OLD.fecha_fin,
            CASE WHEN NOT v_cambio_rotacion AND NEW.estado = 'Esperando cupo'
                 THEN NEW.id_residente END);
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

-- Las filas existentes no tenian fecha de registro: se igualan al momento de
-- la migracion y, entre ellas, el orden de la espera lo decide id_residente.
UPDATE rotacion_residente SET fecha_registro = now();

-- Promueve una vez a quienes ya esperaban en rotaciones con cupo libre.
DO $$
DECLARE v_total INTEGER;
BEGIN
  SELECT COALESCE(SUM(promover_espera_rotacion(r.id_asignatura, r.fecha_inicio, r.fecha_fin)), 0)
  INTO   v_total
  FROM   rotacion r
  WHERE  r.cupo_disponible > 0
    AND  EXISTS (SELECT 1 FROM rotacion_residente rr
                 WHERE rr.id_asignatura = r.id_asignatura AND rr.fecha_inicio = r.fecha_inicio
                   AND rr.fecha_fin = r.fecha_fin AND rr.estado = 'Esperando cupo');
  RAISE NOTICE 'Residentes promovidos desde la lista de espera: %', v_total;
END $$;

COMMIT;
