# Programas: CRUD e importación Excel

El modelo sigue la base final: programa pertenece a una universidad y tiene nombre y estado.
No se modifican tablas ni se añaden categoría o asignatura. Se conserva la arquitectura
controller → service/interfaz → service/impl → repository, con entidades, DTO y mapper.

## Ejecutar en desarrollo

1. En la raíz: `docker compose up -d`.
2. En backend: `.\mvnw.cmd spring-boot:run`.
3. En frontend: `npm install`, después `npm start`.
4. Abrir http://localhost:4200/admin/programas.
   Si 4200 está ocupado, `npm start -- --port 4201` y abrir http://localhost:4201/admin/programas.
   Reiniciar un servidor Angular anterior para que tome la nueva configuración del proxy.

Angular envía las solicitudes a /api y el proxy de desarrollo las dirige a localhost:8080.
Para desplegar el frontend estático se necesita un proxy /api hacia Spring Boot.
CORS de desarrollo admite localhost y 127.0.0.1 en 4200 y 4201.
Producción sigue usando CORS_ALLOWED_ORIGINS.

## Comportamiento

- Listado y búsqueda de programas activos con paginación.
- Crear y editar exigen nombre (no vacío, hasta 150 caracteres) y universidad activa existente.
- Nombre y universidad identifican duplicados; se ignoran diferencias de mayúsculas y espacios exteriores.
- El mismo nombre en universidades diferentes está permitido.
- Eliminar solicita confirmación y cambia estado a Inactivo. No ejecuta DELETE SQL.
- Los residentes y demás relaciones se conservan. Consultar por ID permite comprobar el estado inactivo.
- Un programa inactivo no se puede editar ni duplicar mediante la carga. No se implementa reactivación.
- Las escrituras se realizan en transacciones; bloqueos de universidad serializan la verificación de duplicados.
- Los bloqueos de programa evitan que una edición concurrente reactive accidentalmente un programa desactivado.

## API

| Método | Ruta (prefijo /api) | Resultado |
| --- | --- | --- |
| GET | /programas | Programas activos |
| GET | /programas/{id} | Un programa, incluso inactivo |
| POST | /programas | Crear: 201 |
| PUT | /programas/{id} | Actualizar: 200 |
| DELETE | /programas/{id} | Desactivar: 204 |
| GET | /universidades | Catálogo de universidades activas |
| POST | /programas/importacion/validar | Validar sin guardar |
| POST | /programas/importacion | Guardar filas válidas y devolver resultado por fila |

Crear/editar:

```json
{"nombre":"Especialización de ejemplo","universidadId":1}
```

Carga (el frontend lee Excel; el backend recibe JSON):

```json
{"filas":[{"fila":3,"nombre":"Especialización de ejemplo","universidad":"Pontificia Universidad Javeriana"}]}
```

La importación devuelve total, validos, creados, omitidos, errores y filas.
Cada fila contiene su número original de Excel, nombre, universidad, estado y mensaje.
Estados: VALIDO (previsualización), CREADO, OMITIDO (duplicado) o ERROR.
Al confirmar se vuelve a validar: el resultado puede cambiar si otro usuario importó antes.
Cada creación del lote tiene su propia transacción; una fila incorrecta no deshace las guardadas.
Reintentar un archivo no duplica programas ya creados. La importación no actualiza registros existentes.

Errores individuales: 400 para datos inválidos, 404 para ID inexistente y 409 para duplicados/conflictos.
Formato: `{"mensaje":"...","campos":{"nombre":"..."}}`.
Un lote estructuralmente inválido (vacío, más de 1000 filas, número de fila repetido/nulo) recibe 400.
Las filas con datos inválidos dentro de un lote correcto se describen en el resultado 200.

## Usar Excel

1. Importar Excel y seleccionar un .xlsx (hasta 5 MB).
2. Elegir la hoja y la fila que contiene encabezados.
3. Asociar una columna al nombre y otra a universidad. Deben ser diferentes.
4. Revisar la vista previa de las primeras cinco filas.
5. Validar. Se muestran filas válidas, duplicadas y con errores.
6. Confirmar la cantidad a importar.
7. Descargar errores y omitidos, o volver al listado.

Máximo 1000 registros. Las filas completamente vacías en las dos columnas elegidas se ignoran.
La universidad se busca por nombre completo en el catálogo activo, sin distinguir mayúsculas.
El asistente muestra los nombres aceptados. No crea universidades automáticamente.
No admite .xls ni libros protegidos. Las fórmulas en columnas seleccionadas se deben pegar como valores.
Se incluye descarga de plantilla. ExcelJS se carga solo al leer o descargar Excel para reducir la carga inicial.
Referencia de la biblioteca: https://github.com/exceljs/exceljs

## Pruebas

- Backend: `.\mvnw.cmd test` con PostgreSQL local encendido.
  Las pruebas de integración usan transacciones con rollback: no conservan datos de prueba.
  Las secuencias de PostgreSQL sí pueden avanzar durante una prueba.
- Frontend Programas: `npm test -- --watch=false --include="src/app/features/admin/programas/**/*.spec.ts"`.
- Compilación frontend: `npm run build`.
- Generar Excel ficticio (backend activo): desde frontend, `node scripts/crear-excel-prueba.cjs`.
  Archivo: docs/pruebas/programas-prueba.xlsx.
  Hoja "Carga de prueba", encabezados fila 2, nombre columna C, universidad columna A.
  Primera carga: 2 válidos, 1 duplicado y 2 errores. Si ya se importó, los existentes se omiten.

Verificación manual realizada: creación, edición, desactivación, mapeo manual,
rechazo de columnas repetidas, importación mixta y descarga de informe.
Se comprobó en PostgreSQL la persistencia y el estado Inactivo.
Los registros sintéticos usados en la prueba de navegador llevan prefijo PRUEBA y se desactivan al terminar.

## Límites actuales del proyecto

La autenticación/JWT y la identificación del usuario para historial_auditoria aún no están implementadas.
El borrado lógico conserva información, pero no equivale a registrar eventos de auditoría;
no se inventa un id_usuario para llenar esa tabla.

La suite general de Angular contiene cuatro fallos en pruebas anteriores ajenas a Programas:
app.spec.ts aún espera "Hello, frontend"; los tests de los layouts y landing carecen de providers de rutas.
Las pruebas nuevas de Programas pasan. También hay advertencias de tamaño CSS en pantallas existentes.
