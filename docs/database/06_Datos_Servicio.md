# Datos — Servicios

34 servicios distintos de HUSI.

**Los cupos de este archivo son provisionales y no vienen del hospital.**
El Excel no trae `cupo_maximo_instalado` ni `cupo_simultaneo`, y ambos son
`NOT NULL`. Cada servicio arranca con 50 cupos, simultáneo y máximo
instalado. Reemplázalos cuando la
Oficina de Educación entregue las capacidades reales.

`Cirugía General` fue unificado con `Cirugía`, y `Endocrino` con
`Endocrinología`, porque el Excel los escribe de las dos formas.

```sql
INSERT INTO servicio (id_servicio, nombre, descripcion, cupo_maximo_instalado, cupo_simultaneo) VALUES
    (1, 'Alto Riesgo', 'Cargado desde Excel 2025-2', 50, 50),
    (2, 'Anestesiología', 'Cargado desde Excel 2025-2', 50, 50),
    (3, 'CAICP -Centro de Atención Integral en Cuidados Paliativos', 'Cargado desde Excel 2025-2', 50, 50),
    (4, 'Cardiología', 'Cargado desde Excel 2025-2', 50, 50),
    (5, 'Centro Javeriano de Oncología', 'Cargado desde Excel 2025-2', 50, 50),
    (6, 'Cirugía', 'Cargado desde Excel 2025-2', 50, 50),
    (7, 'Cirugía Plástica', 'Cargado desde Excel 2025-2', 50, 50),
    (8, 'Consulta externa', 'Cargado desde Excel 2025-2', 50, 50),
    (9, 'Dermatología', 'Cargado desde Excel 2025-2', 50, 50),
    (10, 'Endocrinología', 'Cargado desde Excel 2025-2', 50, 50);

INSERT INTO servicio (id_servicio, nombre, descripcion, cupo_maximo_instalado, cupo_simultaneo) VALUES
    (11, 'Gastroenterología', 'Cargado desde Excel 2025-2', 50, 50),
    (12, 'Genética Médica', 'Cargado desde Excel 2025-2', 50, 50),
    (13, 'Geriatría', 'Cargado desde Excel 2025-2', 50, 50),
    (14, 'Ginecología', 'Cargado desde Excel 2025-2', 50, 50),
    (15, 'Infectología', 'Cargado desde Excel 2025-2', 50, 50),
    (16, 'Intellectus', 'Cargado desde Excel 2025-2', 50, 50),
    (17, 'Nefrología', 'Cargado desde Excel 2025-2', 50, 50),
    (18, 'Neumología', 'Cargado desde Excel 2025-2', 50, 50),
    (19, 'Neurocirugía', 'Cargado desde Excel 2025-2', 50, 50),
    (20, 'Neurología', 'Cargado desde Excel 2025-2', 50, 50);

INSERT INTO servicio (id_servicio, nombre, descripcion, cupo_maximo_instalado, cupo_simultaneo) VALUES
    (21, 'Neuropsicogeriatría', 'Cargado desde Excel 2025-2', 50, 50),
    (22, 'Oftalmología', 'Cargado desde Excel 2025-2', 50, 50),
    (23, 'Otorrinolaringología', 'Cargado desde Excel 2025-2', 50, 50),
    (24, 'Patología', 'Cargado desde Excel 2025-2', 50, 50),
    (25, 'Psicogeriatría', 'Cargado desde Excel 2025-2', 50, 50),
    (26, 'Psiquiatría Infantil', 'Cargado desde Excel 2025-2', 50, 50),
    (27, 'Psiquiatría de Enlace', 'Cargado desde Excel 2025-2', 50, 50),
    (28, 'Psiquiatría de Enlace en Oncología', 'Cargado desde Excel 2025-2', 50, 50),
    (29, 'Radiología', 'Cargado desde Excel 2025-2', 50, 50),
    (30, 'Reumatología', 'Cargado desde Excel 2025-2', 50, 50);

INSERT INTO servicio (id_servicio, nombre, descripcion, cupo_maximo_instalado, cupo_simultaneo) VALUES
    (31, 'UCI', 'Cargado desde Excel 2025-2', 50, 50),
    (32, 'Unidad de salud mental', 'Cargado desde Excel 2025-2', 50, 50),
    (33, 'Urgencias', 'Cargado desde Excel 2025-2', 50, 50),
    (34, 'Urología', 'Cargado desde Excel 2025-2', 50, 50);

SELECT setval('servicio_id_servicio_seq', (SELECT max(id_servicio) FROM servicio));
```

---

## Verificación

```sql
SELECT count(*) AS servicios FROM servicio;  -- esperado: 34
SELECT nombre, cupo_simultaneo FROM servicio ORDER BY cupo_simultaneo DESC LIMIT 5;
```
