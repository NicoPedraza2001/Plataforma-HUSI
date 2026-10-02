-- ============================================================
-- Normaliza tipo_documento y perfil_academico del residente en
-- catalogos propios. Para bases creadas antes de este cambio; las
-- nuevas ya lo traen desde database/init.
-- ============================================================

BEGIN;

CREATE TABLE tipo_documento (
    id_tipo_documento  SERIAL PRIMARY KEY,

    codigo             VARCHAR(10)  NOT NULL UNIQUE,
    nombre             VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE perfil_academico (
    id_perfil_academico  SERIAL PRIMARY KEY,

    nombre               VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO tipo_documento (codigo, nombre) VALUES
    ('CC',  'Cédula de ciudadanía'),
    ('TI',  'Tarjeta de identidad'),
    ('CE',  'Cédula de extranjería'),
    ('PA',  'Pasaporte'),
    ('RUT', 'Registro único tributario'),
    ('PEP', 'Permiso especial de permanencia'),
    ('PPT', 'Permiso por protección temporal');

INSERT INTO perfil_academico (nombre) VALUES ('Pregrado'), ('Posgrado');

-- Valores libres que no estan en el catalogo se agregan para no perder datos.
INSERT INTO tipo_documento (codigo, nombre)
SELECT DISTINCT upper(trim(r.tipo_documento)), upper(trim(r.tipo_documento))
FROM residente r
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_documento t WHERE t.codigo = upper(trim(r.tipo_documento))
);

INSERT INTO perfil_academico (nombre)
SELECT DISTINCT initcap(trim(r.perfil_academico))
FROM residente r
WHERE trim(coalesce(r.perfil_academico, '')) <> ''
  AND NOT EXISTS (
    SELECT 1 FROM perfil_academico p
    WHERE lower(p.nombre) = lower(trim(r.perfil_academico))
);

ALTER TABLE residente
    ADD COLUMN id_tipo_documento INTEGER,
    ADD COLUMN id_perfil_academico INTEGER;

UPDATE residente r
SET id_tipo_documento = t.id_tipo_documento
FROM tipo_documento t
WHERE t.codigo = upper(trim(r.tipo_documento));

UPDATE residente r
SET id_perfil_academico = p.id_perfil_academico
FROM perfil_academico p
WHERE lower(p.nombre) = lower(trim(r.perfil_academico));

ALTER TABLE residente
    ALTER COLUMN id_tipo_documento SET NOT NULL,
    ADD CONSTRAINT fk_residente_tipo_documento
        FOREIGN KEY (id_tipo_documento) REFERENCES tipo_documento (id_tipo_documento),
    ADD CONSTRAINT fk_residente_perfil_academico
        FOREIGN KEY (id_perfil_academico) REFERENCES perfil_academico (id_perfil_academico),
    DROP COLUMN tipo_documento,
    DROP COLUMN perfil_academico;

CREATE INDEX idx_residente_tipo_documento ON residente (id_tipo_documento);
CREATE INDEX idx_residente_perfil_academico ON residente (id_perfil_academico);

COMMIT;
