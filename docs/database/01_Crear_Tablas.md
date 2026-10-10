# Base de datos HUSI — Creación de tablas

PostgreSQL 16. Corresponde al diagrama entidad-relación final: `ROTACION`
cuelga de `ASIGNATURA`, y `ASIGNATURA` pertenece a un `SERVICIO`.

Orden de ejecución: este archivo primero, luego `02_Funciones.md` y
`03_Triggers.md`.

---

## 1. ROL

```sql
CREATE TABLE rol (
    id_rol      SERIAL PRIMARY KEY,
    nombre_rol  VARCHAR(50) NOT NULL UNIQUE
);
```

## 2. USUARIO

```sql
CREATE TABLE usuario (
    id_usuario  SERIAL PRIMARY KEY,
    id_rol      INTEGER NOT NULL,

    nombre      VARCHAR(100) NOT NULL,
    apellido    VARCHAR(100) NOT NULL,
    correo      VARCHAR(150) NOT NULL UNIQUE,
    contrasena  VARCHAR(255) NOT NULL,

    estado      VARCHAR(20) NOT NULL DEFAULT 'Activo',

    CONSTRAINT fk_usuario_rol
        FOREIGN KEY (id_rol) REFERENCES rol (id_rol),

    CONSTRAINT chk_usuario_estado
        CHECK (estado IN ('Activo', 'Inactivo', 'Eliminado'))
);

COMMENT ON COLUMN usuario.contrasena IS
    'Hash BCrypt generado por el backend. Nunca texto plano.';
```

## 3. UNIVERSIDAD

```sql
CREATE TABLE universidad (
    id_universidad      SERIAL PRIMARY KEY,

    nombre_universidad  VARCHAR(150) NOT NULL UNIQUE,
    pais                VARCHAR(100) NOT NULL,

    estado              VARCHAR(20) NOT NULL DEFAULT 'Activo',

    CONSTRAINT chk_universidad_estado
        CHECK (estado IN ('Activo', 'Inactivo', 'Eliminado'))
);
```

## 4. PROGRAMA

```sql
CREATE TABLE programa (
    id_programa      SERIAL PRIMARY KEY,
    id_universidad   INTEGER NOT NULL,

    nombre_programa  VARCHAR(150) NOT NULL,

    estado           VARCHAR(20) NOT NULL DEFAULT 'Activo',

    CONSTRAINT fk_programa_universidad
        FOREIGN KEY (id_universidad) REFERENCES universidad (id_universidad),

    CONSTRAINT chk_programa_estado
        CHECK (estado IN ('Activo', 'Inactivo', 'Eliminado')),

    -- Hace idempotente el cargue del Excel: reejecutar el archivo no
    -- duplica programas.
    CONSTRAINT uq_programa_universidad_nombre
        UNIQUE (id_universidad, nombre_programa)
);

CREATE INDEX idx_programa_universidad ON programa (id_universidad);
```

## 5. SERVICIO

```sql
CREATE TABLE servicio (
    id_servicio            SERIAL PRIMARY KEY,

    nombre                 VARCHAR(150) NOT NULL UNIQUE,
    descripcion            TEXT,

    cupo_maximo_instalado  INTEGER NOT NULL,
    cupo_simultaneo        INTEGER NOT NULL,

    estado                 VARCHAR(20) NOT NULL DEFAULT 'Activo',

    CONSTRAINT chk_servicio_cupo_maximo
        CHECK (cupo_maximo_instalado >= 0),

    CONSTRAINT chk_servicio_cupo_simultaneo
        CHECK (cupo_simultaneo >= 0),

    CONSTRAINT chk_servicio_cupo_coherente
        CHECK (cupo_simultaneo <= cupo_maximo_instalado),

    CONSTRAINT chk_servicio_estado
        CHECK (estado IN ('Activo', 'Inactivo', 'Eliminado'))
);

COMMENT ON COLUMN servicio.cupo_simultaneo IS
    'Capacidad que hereda cada rotacion de las asignaturas de este servicio.';
```

## 6. RESIDENTE

```sql
CREATE TABLE residente (
    id_residente      SERIAL PRIMARY KEY,
    id_programa       INTEGER NOT NULL,

    tipo_documento    VARCHAR(30)  NOT NULL DEFAULT 'CC',
    nombre_completo   VARCHAR(200) NOT NULL,
    numero_documento  VARCHAR(50)  NOT NULL UNIQUE,

    perfil_academico  VARCHAR(100),

    estado            VARCHAR(20) NOT NULL DEFAULT 'Activo',

    CONSTRAINT fk_residente_programa
        FOREIGN KEY (id_programa) REFERENCES programa (id_programa),

    CONSTRAINT chk_residente_estado
        CHECK (estado IN ('Activo', 'Inactivo', 'Eliminado'))
);

CREATE INDEX idx_residente_programa ON residente (id_programa);
```

`tipo_documento` queda con valor por defecto `CC` porque el Excel de
rotaciones no trae esa columna. No se restringe con CHECK para no
bloquear documentos extranjeros que lleguen en cargas futuras.

## 7. COORDINADOR

```sql
CREATE TABLE coordinador (
    id_coordinador  SERIAL PRIMARY KEY,

    id_usuario      INTEGER NOT NULL UNIQUE,
    id_servicio     INTEGER NOT NULL,

    estado          VARCHAR(20) NOT NULL DEFAULT 'Activo',

    CONSTRAINT fk_coordinador_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),

    CONSTRAINT fk_coordinador_servicio
        FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio),

    CONSTRAINT chk_coordinador_estado
        CHECK (estado IN ('Activo', 'Inactivo', 'Eliminado'))
);

CREATE INDEX idx_coordinador_servicio ON coordinador (id_servicio);
```

## 8. ASIGNATURA

```sql
CREATE TABLE asignatura (
    id_asignatura      SERIAL PRIMARY KEY,
    id_servicio        INTEGER NOT NULL,

    nombre_asignatura  VARCHAR(200) NOT NULL,

    estado             VARCHAR(20) NOT NULL DEFAULT 'Activo',

    CONSTRAINT fk_asignatura_servicio
        FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio),

    CONSTRAINT chk_asignatura_estado
        CHECK (estado IN ('Activo', 'Inactivo', 'Eliminado')),

    -- Clave de negocio para el cargue: el Excel identifica la asignatura
    -- por nombre dentro de su servicio.
    CONSTRAINT uq_asignatura_servicio_nombre
        UNIQUE (id_servicio, nombre_asignatura)
);

CREATE INDEX idx_asignatura_servicio ON asignatura (id_servicio);
```

## 9. ROTACION

```sql
CREATE TABLE rotacion (
    id_asignatura    INTEGER NOT NULL,

    fecha_inicio     DATE NOT NULL,
    fecha_fin        DATE NOT NULL,

    cupo_disponible  INTEGER NOT NULL,

    estado           VARCHAR(30) NOT NULL DEFAULT 'Disponible',
    activa           BOOLEAN NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id_asignatura, fecha_inicio, fecha_fin),

    CONSTRAINT fk_rotacion_asignatura
        FOREIGN KEY (id_asignatura) REFERENCES asignatura (id_asignatura),

    CONSTRAINT chk_rotacion_fechas
        CHECK (fecha_fin >= fecha_inicio),

    CONSTRAINT chk_rotacion_cupo
        CHECK (cupo_disponible >= 0),

    CONSTRAINT chk_rotacion_estado
        CHECK (estado IN ('Disponible', 'Cupo Lleno'))
);
```

No se restringe el solapamiento de fechas entre rotaciones: los datos
reales de 2025-2 contienen cientos de ventanas traslapadas porque cada
programa académico define sus propias fechas de entrada y salida.

`cupo_disponible` nunca se escribe a mano. Lo mantienen las funciones de
`02_Funciones.md` y un trigger de protección rechaza cualquier `UPDATE`
directo sobre esa columna.

## 10. ROTACION_RESIDENTE

```sql
CREATE TABLE rotacion_residente (
    id_residente   INTEGER NOT NULL,

    id_asignatura  INTEGER NOT NULL,
    fecha_inicio   DATE NOT NULL,
    fecha_fin      DATE NOT NULL,

    estado         VARCHAR(30) NOT NULL DEFAULT 'Esperando cupo',

    PRIMARY KEY (id_residente, id_asignatura, fecha_inicio, fecha_fin),

    CONSTRAINT fk_rr_residente
        FOREIGN KEY (id_residente) REFERENCES residente (id_residente),

    CONSTRAINT fk_rr_rotacion
        FOREIGN KEY (id_asignatura, fecha_inicio, fecha_fin)
        REFERENCES rotacion (id_asignatura, fecha_inicio, fecha_fin),

    CONSTRAINT chk_rr_estado
        CHECK (estado IN ('Asignado', 'Esperando cupo', 'Cancelado'))
);

CREATE INDEX idx_rr_espera
    ON rotacion_residente (id_asignatura, fecha_inicio, fecha_fin, estado);

CREATE INDEX idx_rr_residente ON rotacion_residente (id_residente);
```

## 11. HISTORIAL_AUDITORIA

```sql
CREATE TABLE historial_auditoria (
    id_historial        SERIAL PRIMARY KEY,

    id_usuario          INTEGER NOT NULL,

    accion_realizada    VARCHAR(100) NOT NULL,
    entidad_modificada  VARCHAR(100) NOT NULL,

    fecha               TIMESTAMPTZ NOT NULL DEFAULT now(),

    detalle             TEXT,

    CONSTRAINT fk_historial_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

CREATE INDEX idx_historial_usuario_fecha
    ON historial_auditoria (id_usuario, fecha DESC);
```

---

## 12. Vistas de control

### 12.1 Consistencia del contador por rotación

```sql
CREATE VIEW vw_rotacion_ocupacion AS
SELECT
    r.id_asignatura,
    a.nombre_asignatura,
    s.id_servicio,
    s.nombre                AS nombre_servicio,
    r.fecha_inicio,
    r.fecha_fin,
    s.cupo_simultaneo,
    r.cupo_disponible       AS cupo_almacenado,
    s.cupo_simultaneo - COUNT(rr.*) FILTER (WHERE rr.estado = 'Asignado')
                            AS cupo_calculado,
    COUNT(rr.*) FILTER (WHERE rr.estado = 'Asignado')       AS asignados,
    COUNT(rr.*) FILTER (WHERE rr.estado = 'Esperando cupo') AS en_espera,
    r.estado,
    (r.cupo_disponible =
        s.cupo_simultaneo - COUNT(rr.*) FILTER (WHERE rr.estado = 'Asignado'))
                            AS consistente
FROM rotacion r
JOIN asignatura a ON a.id_asignatura = r.id_asignatura
JOIN servicio   s ON s.id_servicio   = a.id_servicio
LEFT JOIN rotacion_residente rr
       ON rr.id_asignatura = r.id_asignatura
      AND rr.fecha_inicio  = r.fecha_inicio
      AND rr.fecha_fin     = r.fecha_fin
GROUP BY r.id_asignatura, a.nombre_asignatura, s.id_servicio, s.nombre,
         r.fecha_inicio, r.fecha_fin, s.cupo_simultaneo,
         r.cupo_disponible, r.estado;
```

### 12.2 Ocupación real del servicio, día a día

```sql
CREATE VIEW vw_ocupacion_servicio_diaria AS
SELECT
    s.id_servicio,
    s.nombre        AS nombre_servicio,
    d.dia::date     AS dia,
    s.cupo_simultaneo,
    COUNT(*)                             AS residentes_ese_dia,
    COUNT(DISTINCT rr.id_asignatura)     AS asignaturas_activas,
    (COUNT(*) > s.cupo_simultaneo)       AS excede_cupo_servicio
FROM rotacion_residente rr
JOIN asignatura a ON a.id_asignatura = rr.id_asignatura
JOIN servicio   s ON s.id_servicio   = a.id_servicio
CROSS JOIN LATERAL generate_series(rr.fecha_inicio, rr.fecha_fin,
                                   INTERVAL '1 day') AS d(dia)
WHERE rr.estado = 'Asignado'
GROUP BY s.id_servicio, s.nombre, d.dia, s.cupo_simultaneo;
```

Esta vista es la pieza de control más importante del modelo. Como el cupo
se asigna por rotación y cada asignatura genera sus propias rotaciones, un
servicio con N asignaturas puede llegar a alojar N veces su
`cupo_simultaneo`. La vista mide cuántos residentes coinciden realmente en
el servicio cada día y marca los días en exceso.

### 12.3 Pico de ocupación por servicio

```sql
CREATE VIEW vw_pico_ocupacion_servicio AS
SELECT
    id_servicio,
    nombre_servicio,
    cupo_simultaneo,
    MAX(residentes_ese_dia)                    AS pico_residentes,
    COUNT(*) FILTER (WHERE excede_cupo_servicio) AS dias_en_exceso
FROM vw_ocupacion_servicio_diaria
GROUP BY id_servicio, nombre_servicio, cupo_simultaneo;
```

### 12.4 Lista de espera

```sql
CREATE VIEW vw_lista_espera AS
SELECT
    rr.id_asignatura,
    a.nombre_asignatura,
    s.nombre           AS nombre_servicio,
    rr.fecha_inicio,
    rr.fecha_fin,
    rr.id_residente,
    res.nombre_completo,
    p.nombre_programa,
    u.nombre_universidad
FROM rotacion_residente rr
JOIN asignatura  a   ON a.id_asignatura  = rr.id_asignatura
JOIN servicio    s   ON s.id_servicio    = a.id_servicio
JOIN residente   res ON res.id_residente = rr.id_residente
JOIN programa    p   ON p.id_programa    = res.id_programa
JOIN universidad u   ON u.id_universidad = p.id_universidad
WHERE rr.estado = 'Esperando cupo';
```

## Estado administrativo de Rotación

Actualizado 2026-09-14: `activa=true` representa Activo y `activa=false` representa Inactivo. **Activo ↔ Inactivo** es reversible. Se conserva `estado` como disponibilidad (Disponible / Cupo Lleno), independiente del estado administrativo. Eliminar definitivamente es una operación DELETE y no un estado. Requiere que no existan filas relacionadas en `rotacion_residente`, incluso canceladas.

La base existente se actualiza con `database/migrations/20260914_rotacion_activa.sql`; la inicialización nueva aplica `database/init/11_rotacion_activa.sql`. Detalles y pruebas en `docs/rotaciones.md`.
