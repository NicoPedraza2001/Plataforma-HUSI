-- La capacidad de cada rotación se congela al crearla. Solo las transiciones
-- desde Asignado llaman esta función; no se compara con el servicio actual.
CREATE OR REPLACE FUNCTION liberar_cupo_rotacion(p_id_asignatura INTEGER,p_fecha_inicio DATE,p_fecha_fin DATE)
RETURNS VOID AS $$
BEGIN
  PERFORM 1 FROM rotacion WHERE id_asignatura=p_id_asignatura AND fecha_inicio=p_fecha_inicio AND fecha_fin=p_fecha_fin FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'No existe la rotacion (asignatura %, % a %).',p_id_asignatura,p_fecha_inicio,p_fecha_fin USING ERRCODE='foreign_key_violation';
  END IF;
  UPDATE rotacion SET cupo_disponible=cupo_disponible+1,estado='Disponible'
  WHERE id_asignatura=p_id_asignatura AND fecha_inicio=p_fecha_inicio AND fecha_fin=p_fecha_fin;
END;
$$ LANGUAGE plpgsql;
