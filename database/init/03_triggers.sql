-- ============================================================
-- Triggers del ciclo de vida del cupo
-- Se ejecuta automaticamente al levantar el contenedor 
-- ============================================================

CREATE TRIGGER trg_inicializar_cupo_rotacion
    BEFORE INSERT ON rotacion
    FOR EACH ROW
    EXECUTE FUNCTION inicializar_cupo_rotacion();

CREATE TRIGGER trg_rotacion_proteger_cupo
    BEFORE UPDATE OF cupo_disponible ON rotacion
    FOR EACH ROW
    WHEN (OLD.cupo_disponible IS DISTINCT FROM NEW.cupo_disponible)
    EXECUTE FUNCTION trg_fn_rotacion_proteger_cupo();

CREATE TRIGGER trg_rr_insert
    BEFORE INSERT ON rotacion_residente
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_rr_insert();

CREATE TRIGGER trg_rr_update
    AFTER UPDATE ON rotacion_residente
    FOR EACH ROW
    WHEN (
           OLD.estado        IS DISTINCT FROM NEW.estado
        OR OLD.id_asignatura IS DISTINCT FROM NEW.id_asignatura
        OR OLD.fecha_inicio  IS DISTINCT FROM NEW.fecha_inicio
        OR OLD.fecha_fin     IS DISTINCT FROM NEW.fecha_fin
    )
    EXECUTE FUNCTION trg_fn_rr_update();

CREATE TRIGGER trg_rr_delete
    AFTER DELETE ON rotacion_residente
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_rr_delete();

