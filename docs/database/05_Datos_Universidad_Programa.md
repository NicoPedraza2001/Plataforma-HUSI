# Datos — Universidad y programas

Una universidad y 21 programas académicos, tomados de la
columna `Programa` del Excel, que trae el nombre completo. La columna
`SAE_Programa` solo contiene el código corto (`ECIMA`, `ORONT`, `ESRAD`),
y se usa aquí únicamente para agrupar: el archivo escribe el mismo programa
con los conectores en mayúscula o minúscula según la fila, y `ESRAD` llega
a tener tres variantes. Se unifica cada código a su forma correcta.

Los `id_programa` van explícitos para que los archivos siguientes puedan
referenciarlos sin consultar la base. Al final se resincronizan las
secuencias.

```sql
INSERT INTO universidad (id_universidad, nombre_universidad, pais) VALUES
    (1, 'Pontificia Universidad Javeriana', 'Colombia');

INSERT INTO programa (id_programa, id_universidad, nombre_programa) VALUES
    (1, 1, 'Especialización en Anestesiología'),
    (2, 1, 'Especialización en Cirugía Cardiovascular'),
    (3, 1, 'Especialización en Cirugía General'),
    (4, 1, 'Especialización en Cirugía Oncológica'),
    (5, 1, 'Especialización en Cirugía Plástica: Reconstructiva y Estética'),
    (6, 1, 'Especialización en Cirugía de Mano'),
    (7, 1, 'Especialización en Dermatología'),
    (8, 1, 'Especialización en Geriatría'),
    (9, 1, 'Especialización en Ginecología y Obstetricia'),
    (10, 1, 'Especialización en Mastología'),
    (11, 1, 'Especialización en Medicina Crítica y Cuidado Intensivo'),
    (12, 1, 'Especialización en Medicina Interna'),
    (13, 1, 'Especialización en Neurocirugía'),
    (14, 1, 'Especialización en Oftalmología'),
    (15, 1, 'Especialización en Ortopedia y Traumatología'),
    (16, 1, 'Especialización en Otorrinolaringología'),
    (17, 1, 'Especialización en Patología'),
    (18, 1, 'Especialización en Psiquiatría'),
    (19, 1, 'Especialización en Psiquiatría Geriátrica'),
    (20, 1, 'Especialización en Radiología e Imágenes Diagnósticas'),
    (21, 1, 'Especialización en Urología');

SELECT setval('universidad_id_universidad_seq', (SELECT max(id_universidad) FROM universidad));
SELECT setval('programa_id_programa_seq', (SELECT max(id_programa) FROM programa));
```

---

## Verificación

```sql
SELECT count(*) AS programas FROM programa;  -- esperado: 21
```
