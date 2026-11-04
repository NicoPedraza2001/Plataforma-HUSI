-- ============================================================
-- El historial conserva quien hizo cada cambio aunque su cuenta se
-- elimine: guarda el nombre y el correo del responsable en la fila, y
-- id_usuario pasa a NULL al eliminar el usuario. Para bases creadas
-- antes de este cambio; las nuevas ya lo traen desde database/init.
-- ============================================================

BEGIN;

ALTER TABLE historial_auditoria
    ADD COLUMN IF NOT EXISTS nombre_responsable VARCHAR(201),
    ADD COLUMN IF NOT EXISTS correo_responsable VARCHAR(150);

UPDATE historial_auditoria h
SET    nombre_responsable = concat(u.nombre, ' ', u.apellido),
       correo_responsable = u.correo
FROM   usuario u
WHERE  u.id_usuario = h.id_usuario
  AND  h.nombre_responsable IS NULL;

ALTER TABLE historial_auditoria
    ALTER COLUMN nombre_responsable SET NOT NULL,
    ALTER COLUMN correo_responsable SET NOT NULL,
    ALTER COLUMN id_usuario DROP NOT NULL;

ALTER TABLE historial_auditoria DROP CONSTRAINT IF EXISTS fk_historial_usuario;
ALTER TABLE historial_auditoria
    ADD CONSTRAINT fk_historial_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE SET NULL;

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
        id_usuario, nombre_responsable, correo_responsable,
        accion_realizada, entidad_modificada, detalle)
    SELECT
        v_id_usuario, concat(nombre, ' ', apellido), correo,
        p_accion, p_entidad, p_detalle
    FROM usuario
    WHERE id_usuario = v_id_usuario;
END;
$$ LANGUAGE plpgsql;

COMMIT;
