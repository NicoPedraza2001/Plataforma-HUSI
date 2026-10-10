-- ============================================================
-- Elimina la columna perfil de universidad: era opcional y no
-- se usaba en ninguna regla del sistema. Para bases creadas
-- antes de este cambio; las nuevas ya no la traen desde
-- database/init.
-- ============================================================

BEGIN;

ALTER TABLE universidad DROP COLUMN IF EXISTS perfil;

COMMIT;
