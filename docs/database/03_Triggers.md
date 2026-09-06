# Base de datos HUSI — Triggers

Ejecutar después de `02_Funciones.md`.

---

## 1. Inicialización del cupo de la rotación

```sql
CREATE TRIGGER trg_inicializar_cupo_rotacion
    BEFORE INSERT ON rotacion
    FOR EACH ROW
    EXECUTE FUNCTION inicializar_cupo_rotacion();
```

Se dispara al crear cada rotación. El backend inserta solo
`id_asignatura`, `fecha_inicio` y `fecha_fin`; el trigger llena
`cupo_disponible` y `estado`. Por eso `cupo_disponible` puede ser `NOT NULL`
sin que el `INSERT` lo envíe: PostgreSQL valida las restricciones después de
ejecutar los triggers `BEFORE`.

---

## 2. Protección del contador

```sql
CREATE TRIGGER trg_rotacion_proteger_cupo
    BEFORE UPDATE OF cupo_disponible ON rotacion
    FOR EACH ROW
    WHEN (OLD.cupo_disponible IS DISTINCT FROM NEW.cupo_disponible)
    EXECUTE FUNCTION trg_fn_rotacion_proteger_cupo();
```

Bloquea cualquier intento de escribir el contador desde fuera de las
funciones de cupo: pgAdmin, una migración, un `UPDATE` accidental del
backend o el cargue masivo del Excel.

---

## 3. Alta de una asignación

```sql
CREATE TRIGGER trg_rr_insert
    BEFORE INSERT ON rotacion_residente
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_rr_insert();
```

Valida que el residente exista y esté activo, y decide el estado inicial:
`Asignado` si hay cupo, `Esperando cupo` si no. El backend no necesita
consultar disponibilidad antes de insertar.

---

## 4. Cambio de estado o de rotación

```sql
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
```

La cláusula `WHEN` es la que evita el ciclo. Sin ella, la promoción de un
residente desde la lista de espera dispararía la liberación del cupo que
acaba de tomar. Además impide que el trigger se ejecute en `UPDATE` que no
tienen nada que ver con la capacidad.

---

## 5. Baja física de una asignación

```sql
CREATE TRIGGER trg_rr_delete
    AFTER DELETE ON rotacion_residente
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_rr_delete();
```

Lo normal es cancelar con `UPDATE ... SET estado = 'Cancelado'`, que
conserva el histórico. Pero si alguien borra la fila, el cupo igual regresa.

---

## Resumen del ciclo de vida del cupo

| Operación del backend | Trigger que actúa | Efecto sobre `cupo_disponible` |
|---|---|---|
| `INSERT` en `rotacion` | `trg_inicializar_cupo_rotacion` | Se fija en `servicio.cupo_simultaneo` |
| `INSERT` en `rotacion_residente` | `trg_rr_insert` | −1 si hay cupo; sin cambio si queda en espera |
| `UPDATE` a `'Cancelado'` | `trg_rr_update` | +1 |
| `UPDATE` de espera a `'Asignado'` | `trg_rr_update` | −1, o error si no hay cupo |
| `UPDATE` de rotación (cambio de fechas o asignatura) | `trg_rr_update` | +1 en la vieja, −1 en la nueva |
| `DELETE` de un asignado | `trg_rr_delete` | +1 |
| `UPDATE` directo del contador | `trg_rotacion_proteger_cupo` | Rechazado |

---

## Verificación

Después de cualquier operación, esta consulta debe devolver cero:

```sql
SELECT count(*) FROM vw_rotacion_ocupacion WHERE consistente IS FALSE;
```

Y esta muestra si algún servicio está recibiendo más residentes de los que
declara soportar, sumando todas sus asignaturas:

```sql
SELECT * FROM vw_pico_ocupacion_servicio WHERE dias_en_exceso > 0;
```
