-- ============================================================
-- Historial de auditoria con detalle del cambio: registro afectado,
-- datos anteriores y nuevos (JSON) y lote de importacion para resumir
-- una importacion completa en una sola fila. Para bases creadas antes
-- de este cambio; las nuevas ya lo traen desde database/init.
-- ============================================================

BEGIN;

ALTER TABLE historial_auditoria
    ADD COLUMN IF NOT EXISTS registro         VARCHAR(250),
    ADD COLUMN IF NOT EXISTS datos_anteriores TEXT,
    ADD COLUMN IF NOT EXISTS datos_nuevos     TEXT,
    ADD COLUMN IF NOT EXISTS lote_importacion VARCHAR(36);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_historial_lote_importacion'
    ) THEN
        ALTER TABLE historial_auditoria
            ADD CONSTRAINT uq_historial_lote_importacion UNIQUE (lote_importacion);
    END IF;
END $$;

COMMIT;
