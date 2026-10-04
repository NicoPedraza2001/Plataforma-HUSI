-- ============================================================
-- Tablas, indices y vistas de control
-- Se ejecuta automaticamente al levantar el contenedor
-- ============================================================

CREATE TABLE rol (
    id_rol      SERIAL PRIMARY KEY,
    nombre_rol  VARCHAR(50) NOT NULL UNIQUE
);

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



CREATE TABLE universidad (
    id_universidad      SERIAL PRIMARY KEY,

    nombre_universidad  VARCHAR(150) NOT NULL UNIQUE,
    pais                VARCHAR(100) NOT NULL,
    perfil              VARCHAR(50),

    estado              VARCHAR(20) NOT NULL DEFAULT 'Activo',

    CONSTRAINT chk_universidad_estado
        CHECK (estado IN ('Activo', 'Inactivo', 'Eliminado'))
);

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



CREATE TABLE tipo_documento (
    id_tipo_documento  SERIAL PRIMARY KEY,
    codigo             VARCHAR(10)  NOT NULL UNIQUE,
    nombre             VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE perfil_academico (
    id_perfil_academico  SERIAL PRIMARY KEY,
    nombre               VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE residente (
    id_residente         SERIAL PRIMARY KEY,
    id_programa          INTEGER NOT NULL,
    id_tipo_documento    INTEGER NOT NULL,
    id_perfil_academico  INTEGER,

    nombre_completo      VARCHAR(200) NOT NULL,
    numero_documento     VARCHAR(50)  NOT NULL UNIQUE,

    estado               VARCHAR(20) NOT NULL DEFAULT 'Activo',

    CONSTRAINT fk_residente_programa
        FOREIGN KEY (id_programa) REFERENCES programa (id_programa),

    CONSTRAINT fk_residente_tipo_documento
        FOREIGN KEY (id_tipo_documento) REFERENCES tipo_documento (id_tipo_documento),

    CONSTRAINT fk_residente_perfil_academico
        FOREIGN KEY (id_perfil_academico) REFERENCES perfil_academico (id_perfil_academico),

    CONSTRAINT chk_residente_estado
        CHECK (estado IN ('Activo', 'Inactivo', 'Eliminado'))
);

CREATE INDEX idx_residente_programa ON residente (id_programa);

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

CREATE TABLE rotacion (
    id_asignatura    INTEGER NOT NULL,

    fecha_inicio     DATE NOT NULL,
    fecha_fin        DATE NOT NULL,

    cupo_disponible  INTEGER NOT NULL DEFAULT 0,

    estado           VARCHAR(30) NOT NULL DEFAULT 'Disponible',

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

CREATE TABLE rotacion_residente (
    id_residente   INTEGER NOT NULL,

    id_asignatura  INTEGER NOT NULL,
    fecha_inicio   DATE NOT NULL,
    fecha_fin      DATE NOT NULL,

    estado         VARCHAR(30) NOT NULL DEFAULT 'Esperando cupo',

    fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

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

CREATE TABLE historial_auditoria (
    id_historial        SERIAL PRIMARY KEY,

    id_usuario          INTEGER NOT NULL,

    accion_realizada    VARCHAR(100) NOT NULL,
    entidad_modificada  VARCHAR(100) NOT NULL,

    fecha               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    detalle             TEXT,

    CONSTRAINT fk_historial_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

CREATE INDEX idx_historial_usuario_fecha
    ON historial_auditoria (id_usuario, fecha DESC);


ALTER TABLE rotacion ADD activa BOOLEAN NOT NULL DEFAULT TRUE;
CREATE TRIGGER local_rotacion BEFORE INSERT, UPDATE ON rotacion FOR EACH ROW CALL 'co.edu.javeriana.husi.config.local.CupoRotacionTrigger';
CREATE TRIGGER local_asignacion BEFORE INSERT, UPDATE, DELETE ON rotacion_residente FOR EACH ROW CALL 'co.edu.javeriana.husi.config.local.AsignacionTrigger';
CREATE TRIGGER local_rotacion_reactivada AFTER UPDATE ON rotacion FOR EACH ROW CALL 'co.edu.javeriana.husi.config.local.RotacionReactivadaTrigger';
CREATE TRIGGER local_servicio_cupo AFTER UPDATE ON servicio FOR EACH ROW CALL 'co.edu.javeriana.husi.config.local.CupoServicioTrigger';
