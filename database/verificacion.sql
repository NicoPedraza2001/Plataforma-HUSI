-- ============================================================
-- Verificacion de la carga inicial
--
-- NO se ejecuta automaticamente. Se corre a mano despues de levantar el
-- contenedor, desde pgAdmin o con:
--
--   docker compose exec -T postgres psql -U husi -d husi -f - < database/verificacion.sql
--
-- Los valores esperados vienen de docs/database/*.md.
-- ============================================================

-- 1. Conteos por tabla -------------------------------------------------
SELECT 'universidad'        AS tabla, count(*) AS filas, 1   AS esperado FROM universidad
UNION ALL SELECT 'programa',            count(*), 21  FROM programa
UNION ALL SELECT 'servicio',            count(*), 34  FROM servicio
UNION ALL SELECT 'asignatura',          count(*), 229 FROM asignatura
UNION ALL SELECT 'residente',           count(*), 214 FROM residente
UNION ALL SELECT 'rotacion',            count(*), 586 FROM rotacion
UNION ALL SELECT 'rotacion_residente',  count(*), 712 FROM rotacion_residente
UNION ALL SELECT 'rol',                 count(*), 2   FROM rol
UNION ALL SELECT 'usuario',             count(*), 1   FROM usuario
ORDER BY tabla;

-- 2. Estado de las asignaciones ---------------------------------------
-- Esperado: Asignado = 712, sin filas en 'Esperando cupo'.
SELECT estado, count(*) FROM rotacion_residente GROUP BY estado ORDER BY estado;

-- 3. Estado de las rotaciones -----------------------------------------
SELECT estado, count(*) FROM rotacion GROUP BY estado ORDER BY estado;

-- 4. Consistencia del contador de cupo ---------------------------------
-- Esperado: 0.
SELECT count(*) AS inconsistentes
FROM vw_rotacion_ocupacion
WHERE consistente IS FALSE;

-- 5. Servicios que reciben mas residentes de los que declaran soportar --
-- Sumando todas sus asignaturas. Es un indicador, no un error de carga.
SELECT * FROM vw_pico_ocupacion_servicio
WHERE dias_en_exceso > 0
ORDER BY dias_en_exceso DESC;

-- 6. Lista de espera ---------------------------------------------------
-- Esperado: vacia con los cupos actuales.
SELECT count(*) AS en_espera FROM vw_lista_espera;

-- 7. Asignaturas repetidas en mas de un servicio -----------------------
-- Esperado: 26 nombres repartidos en varios servicios (ver 07_Datos_Asignatura.md).
SELECT nombre_asignatura, count(*) AS servicios
FROM asignatura
GROUP BY nombre_asignatura
HAVING count(*) > 1
ORDER BY 2 DESC, 1;

-- 8. Usuario inicial ---------------------------------------------------
SELECT r.nombre_rol, u.correo, u.estado
FROM usuario u
JOIN rol r ON r.id_rol = u.id_rol;
