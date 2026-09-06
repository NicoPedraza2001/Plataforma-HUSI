-- ============================================================
-- Datos: servicios del hospital
-- Se ejecuta automaticamente al levantar el contenedor 
-- ============================================================

-- PENDIENTE: los cupos de este archivo son provisionales y no vienen del
-- hospital. El Excel no trae cupo_maximo_instalado ni cupo_simultaneo, y
-- ambos son NOT NULL. El valor usado es la mayor cantidad de residentes que
-- comparten una misma rotacion en ese servicio, es decir, el minimo que
-- permite cargar los datos sin mandar a nadie a lista de espera.
-- Reemplazarlos cuando la Oficina de Educacion entregue las capacidades
-- reales.
--
-- 'Cirugia General' quedo unificado con 'Cirugia', y 'Endocrino' con
-- 'Endocrinologia', porque el Excel los escribe de las dos formas.

INSERT INTO servicio (id_servicio, nombre, descripcion, cupo_maximo_instalado, cupo_simultaneo) VALUES
    (1, 'Alto Riesgo', 'Cargado desde Excel 2025-2', 10, 10),
    (2, 'Anestesiología', 'Cargado desde Excel 2025-2', 1, 1),
    (3, 'CAICP -Centro de Atención Integral en Cuidados Paliativos', 'Cargado desde Excel 2025-2', 2, 2),
    (4, 'Cardiología', 'Cargado desde Excel 2025-2', 2, 2),
    (5, 'Centro Javeriano de Oncología', 'Cargado desde Excel 2025-2', 2, 2),
    (6, 'Cirugía', 'Cargado desde Excel 2025-2', 1, 1),
    (7, 'Cirugía Plástica', 'Cargado desde Excel 2025-2', 1, 1),
    (8, 'Consulta externa', 'Cargado desde Excel 2025-2', 1, 1),
    (9, 'Dermatología', 'Cargado desde Excel 2025-2', 1, 1),
    (10, 'Endocrinología', 'Cargado desde Excel 2025-2', 2, 2);

INSERT INTO servicio (id_servicio, nombre, descripcion, cupo_maximo_instalado, cupo_simultaneo) VALUES
    (11, 'Gastroenterología', 'Cargado desde Excel 2025-2', 1, 1),
    (12, 'Genética Médica', 'Cargado desde Excel 2025-2', 1, 1),
    (13, 'Geriatría', 'Cargado desde Excel 2025-2', 4, 4),
    (14, 'Ginecología', 'Cargado desde Excel 2025-2', 1, 1),
    (15, 'Infectología', 'Cargado desde Excel 2025-2', 1, 1),
    (16, 'Intellectus', 'Cargado desde Excel 2025-2', 2, 2),
    (17, 'Nefrología', 'Cargado desde Excel 2025-2', 2, 2),
    (18, 'Neumología', 'Cargado desde Excel 2025-2', 2, 2),
    (19, 'Neurocirugía', 'Cargado desde Excel 2025-2', 2, 2),
    (20, 'Neurología', 'Cargado desde Excel 2025-2', 2, 2);

INSERT INTO servicio (id_servicio, nombre, descripcion, cupo_maximo_instalado, cupo_simultaneo) VALUES
    (21, 'Neuropsicogeriatría', 'Cargado desde Excel 2025-2', 1, 1),
    (22, 'Oftalmología', 'Cargado desde Excel 2025-2', 1, 1),
    (23, 'Otorrinolaringología', 'Cargado desde Excel 2025-2', 2, 2),
    (24, 'Patología', 'Cargado desde Excel 2025-2', 1, 1),
    (25, 'Psicogeriatría', 'Cargado desde Excel 2025-2', 2, 2),
    (26, 'Psiquiatría Infantil', 'Cargado desde Excel 2025-2', 1, 1),
    (27, 'Psiquiatría de Enlace', 'Cargado desde Excel 2025-2', 2, 2),
    (28, 'Psiquiatría de Enlace en Oncología', 'Cargado desde Excel 2025-2', 2, 2),
    (29, 'Radiología', 'Cargado desde Excel 2025-2', 1, 1),
    (30, 'Reumatología', 'Cargado desde Excel 2025-2', 2, 2);

INSERT INTO servicio (id_servicio, nombre, descripcion, cupo_maximo_instalado, cupo_simultaneo) VALUES
    (31, 'UCI', 'Cargado desde Excel 2025-2', 1, 1),
    (32, 'Unidad de salud mental', 'Cargado desde Excel 2025-2', 2, 2),
    (33, 'Urgencias', 'Cargado desde Excel 2025-2', 2, 2),
    (34, 'Urología', 'Cargado desde Excel 2025-2', 1, 1);

SELECT setval('servicio_id_servicio_seq', (SELECT max(id_servicio) FROM servicio));

