BEGIN;
ALTER TABLE rotacion ADD COLUMN IF NOT EXISTS activa BOOLEAN NOT NULL DEFAULT TRUE;
COMMENT ON COLUMN rotacion.activa IS 'Estado administrativo reversible; independiente de Disponible/Cupo Lleno. Eliminar es una operación DELETE, no un estado.';

CREATE OR REPLACE FUNCTION validar_rotacion_activa_rr() RETURNS TRIGGER AS $$
DECLARE v_activa BOOLEAN;
BEGIN
  IF TG_OP = 'UPDATE' THEN
    IF (NEW.id_asignatura,NEW.fecha_inicio,NEW.fecha_fin,NEW.estado)
       IS NOT DISTINCT FROM (OLD.id_asignatura,OLD.fecha_inicio,OLD.fecha_fin,OLD.estado) THEN RETURN NEW; END IF;
  END IF;
  IF NEW.estado = 'Cancelado' THEN RETURN NEW; END IF;
  SELECT activa INTO v_activa FROM rotacion
    WHERE id_asignatura=NEW.id_asignatura AND fecha_inicio=NEW.fecha_inicio AND fecha_fin=NEW.fecha_fin FOR UPDATE;
  IF v_activa IS FALSE THEN
    RAISE EXCEPTION 'La rotacion esta inactiva; no admite nuevas asignaciones ni cambios de asignacion. Puede cancelar las existentes.' USING ERRCODE='check_violation';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trg_rr_00_rotacion_activa ON rotacion_residente;
CREATE TRIGGER trg_rr_00_rotacion_activa BEFORE INSERT OR UPDATE ON rotacion_residente
FOR EACH ROW EXECUTE FUNCTION validar_rotacion_activa_rr();
COMMIT;
