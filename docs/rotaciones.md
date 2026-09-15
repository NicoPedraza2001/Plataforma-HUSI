# Rotaciones: administración e importación

Implementación y verificación: 14 de septiembre de 2026.

## Modelo encontrado y decisiones

La tabla `rotacion` utiliza la clave compuesta `(id_asignatura, fecha_inicio, fecha_fin)`. Pertenece a una asignatura, que pertenece a un servicio. `rotacion_residente` referencia esa clave sin borrado en cascada. El endpoint antiguo `GET /api/rotaciones` devuelve asignaciones de residentes y se conserva para el dashboard. No existían entidad JPA ni CRUD/pantalla de administración de rotaciones.

Se añadió `activa BOOLEAN NOT NULL DEFAULT TRUE`. Los registros existentes quedan activos. El campo anterior `estado`, con `Disponible / Cupo Lleno`, conserva su significado de disponibilidad y sus triggers. No se sustituyó por estados administrativos.

| Operación | Efecto | Relaciones |
| --- | --- | --- |
| Crear | Rotación activa; cupos inicializados por el trigger del servicio | Asignatura y servicio existentes y activos |
| Editar | Cambia las fechas, manteniendo cupos y estado | Bloqueado si hay residentes relacionados; asignatura inmutable |
| Desactivar | `activa=false`; conserva el registro | Conserva residentes, incluso asignados; impide nuevas asignaciones |
| Activar | `activa=true` | Exige asignatura y servicio activos; no modifica residentes |
| Eliminar definitivamente | DELETE físico, tras confirmación en la pantalla | Bloqueado si existe cualquier fila en `rotacion_residente`, incluso cancelada |

**Activo ↔ Inactivo** son reversibles. **Eliminar definitivamente** es una operación, no un estado. Una rotación puede estar activa con cupo lleno o inactiva con cupos disponibles.

Las fechas son parte de la identidad. Para proteger referencias e historial no se cambian cuando hay residentes relacionados. La asignatura determina el servicio y sus cupos: para cambiarla se debe crear otra rotación. Desactivar no cancela las asignaciones previas. Se permite cancelar una asignación en una rotación inactiva; al liberar su cupo permanece inactiva.

Las escrituras bloquean la rotación durante la transacción. La comprobación de dependencias y las claves foráneas permanecen activas. No se borran residentes ni se deshabilitan restricciones. El nuevo trigger bloquea INSERT/UPDATE de asignaciones no canceladas a rotaciones inactivas.

## Archivos creados

Backend, bajo `backend/src/main/java/co/edu/javeriana/husi/`:

- `model/entity/Rotacion.java`, `RotacionId.java`.
- `dto/request/RotacionRequest.java`, `EstadoRotacionRequest.java`, `ImportacionRotacionesRequest.java`.
- `repository/RotacionRepository.java`, `RotacionConsultaRepository.java`.
- `service/RotacionService.java`, `impl/RotacionServiceImpl.java`, `ImportacionRotacionesService.java`.
- `controller/RotacionController.java`.
- Pruebas: `backend/src/test/java/co/edu/javeriana/husi/service/RotacionIntegrationTests.java`.

Frontend:

- `src/app/features/admin/rotaciones/listado-rotaciones.ts`, `.html`, `.css`, `.spec.ts`.
- `src/app/features/admin/rotaciones/importar-rotaciones.ts`, `.html`, `.css`, `.spec.ts`.
- `src/app/features/admin/rotaciones/importar-rotaciones.real-test.ts`: prueba optativa contra el servidor real.
- `src/app/shared/excel/archivo-excel.ts`: lectura/exportación Excel reutilizable.
- `tsconfig.rotaciones-e2e.json`: configuración de la prueba optativa.

Base y documentación:

- `database/migrations/20260914_rotacion_activa.sql`.
- `database/init/11_rotacion_activa.sql`.
- `docs/rotaciones.md` (este informe).

## Archivos modificados

- `frontend/src/app/app.routes.ts`: ruta `/admin/rotaciones`.
- `frontend/src/app/services/rotaciones-api.ts`: centraliza todas las llamadas del módulo; mantiene `listar()` para el dashboard.
- `frontend/src/app/features/admin/programas/excel-programas.ts`: reexporta el lector compartido y conserva el mapeo específico de programas.
- `frontend/tsconfig.app.json`: excluye la prueba optativa de la aplicación de producción.
- `database/README.md`: instrucciones de migración.
- `docs/database/01_Crear_Tablas.md`: representación actualizada de `activa` y explicación del modelo.

No se eliminaron archivos. Se conservó el importador de programas y sus servicios/endpoints para reutilización posterior; no volvió a aparecer en Universidades o Programas. Los archivos de cambios anteriores siguen pendientes de commit junto con este bloque.

## Endpoints

Base nueva: `/api/rotaciones/gestion`.

| Método y ruta | Uso |
| --- | --- |
| GET base | Listado paginado; `q`, `activa=true/false`, `servicioId`, `pagina`, `tamano` |
| GET `/catalogos` | Asignaturas activas con servicio activo; servicios para filtrar |
| GET `/{asignatura}/{inicio}/{fin}` | Consultar una rotación |
| POST base | Crear: `asignaturaId`, `inicio`, `fin` |
| PUT `/{asignatura}/{inicio}/{fin}` | Editar fechas usando la clave anterior en la ruta |
| PATCH `/{asignatura}/{inicio}/{fin}/estado` | Cuerpo `{ "activa": true/false }` |
| DELETE `/{asignatura}/{inicio}/{fin}` | Borrado físico; 409 si hay dependencias |
| POST `/importar/validar` | Validar filas sin guardar |
| POST `/importar` | Revalidar y guardar filas válidas |

Fechas ISO `AAAA-MM-DD`. Los filtros se validan y se enlazan como parámetros SQL. Paginación y búsqueda se ejecutan en PostgreSQL. Errores de datos: 400; inexistencia: 404; duplicados/dependencias: 409. Se reutiliza el manejador de errores del proyecto.

## Excel

El importador anterior solamente creaba programas en universidades ya existentes; no hacía una carga completa de entidades. El nuevo importador crea **rotaciones**, relacionando **asignatura y servicio existentes**. No crea residentes, universidades, programas, asignaturas, servicios ni asignaciones de residentes automáticamente.

Flujo: archivo `.xlsx` → hoja y fila de encabezados → cuatro columnas distintas (asignatura, servicio, inicio, fin) → validación backend → confirmación → resultado por fila.

- Archivos hasta 5 MB; máximo 1000 filas de datos; límite de lectura de 1100 filas/100 columnas por hoja.
- Fechas reales de Excel se convierten a ISO; texto debe ser `AAAA-MM-DD`. Las fórmulas se marcan como inválidas; no se ejecutan.
- Filas vacías se ignoran. Duplicados en archivo o BD se omiten sin editar ni reactivar el registro existente.
- Cada fila válida se guarda en su propia transacción. Una fila errónea no borra las anteriores.
- Si se pierde la conexión durante el guardado, revalidar permite identificar lo ya creado.
- Incluye quitar archivo, plantilla descargable y resultado descargable.
- Cambiar columnas invalida la validación anterior. El usuario confirma antes de persistir.
- La paginación local del informe de importación muestra su resultado puntual; el listado de rotaciones siempre consulta backend.

## Migración y modelo editable

La migración se aplicó en la base local existente dentro de una transacción, sin reinicializar Docker ni borrar volúmenes. En otra base existente ejecutar `database/migrations/20260914_rotacion_activa.sql` con psql y `ON_ERROR_STOP=1` antes de levantar el nuevo backend. En una base nueva Docker ejecuta `database/init/11_rotacion_activa.sql` después de los datos iniciales.

No se encontró un archivo de diagrama editable (drawio, puml, mmd o mermaid) en el repositorio. Se actualizó la representación SQL/Markdown de tablas. En el diagrama externo compartido por imagen: añadir `activa: boolean` a Rotación, conservar `estado` como disponibilidad, representar `Activo ↔ Inactivo`, y mostrar `Eliminar definitivamente` como operación que exige cero dependencias; no como tercer estado. No cambiar la clave compuesta ni las relaciones.

## Pruebas realizadas

- Frontend: 35 pruebas aprobadas (22 archivos), incluyendo cancelar/confirmar eliminación, paginación HTTP, error backend, lectura XLSX con fecha real y fórmulas, mapeo de columnas y confirmación de importación.
- Backend: 20 pruebas aprobadas, incluyendo siete nuevas pruebas PostgreSQL de CRUD, estados, cupos, restricciones incluso con residentes cancelados, bloqueo de asignación a inactivas, búsqueda/paginación, fechas y duplicados, importación y reintentos. Las pruebas automáticas de servicio se revierten al terminar.
- Prueba adicional real: **1 aprobada**, con XLSX generado en memoria, componente Angular, HttpClient real, backend local y PostgreSQL. Validación no guardó; importación creó una fila, omitió duplicado e informó fecha inválida; reintento no duplicó. Se eliminó solamente su registro sintético.
- Navegador en `http://localhost:4200/admin/rotaciones`: listado real (586 rotaciones antes de la prueba), validación de campos obligatorios, creación, búsqueda, edición de fecha, desactivar, reactivar, cancelar eliminación y confirmar eliminación.
- PostgreSQL confirmó la rotación de prueba `(1, 2093-01-01, 2093-01-31)`, su edición a fin `2093-02-01`, inactividad sin perder cupos, reactivación y conservación tras cancelar. Tras confirmar eliminación, conteo cero.
- Compilación de producción del frontend y compilación/pruebas backend correctas. Los procesos locales se verificaron en puertos 4200 y 8080; Docker PostgreSQL estaba saludable.

Prueba real optativa (requiere backend y PostgreSQL):

```powershell
cd frontend
npx ng test --watch=false --ts-config=tsconfig.rotaciones-e2e.json --include=src/app/features/admin/rotaciones/importar-rotaciones.real-test.ts
```

La prueba exige que no exista previamente la rotación de prueba de fechas `2094-09-14` a `2094-09-15` en la primera asignatura del catálogo; si existe, falla antes de modificarla.

## Límites y siguientes decisiones

- La carga inicial completa de todas las entidades del hospital y la asignación masiva de residentes siguen siendo un bloque distinto: faltan reglas/campos de creación para esas entidades. Esta importación está limitada a rotaciones, con el alcance visible en la pantalla.
- Autenticación, permisos y auditoría con usuario autenticado continúan pendientes en el proyecto. No se inventó un usuario para auditar. Implementarlos antes de exponer escritura/borrado fuera del entorno de desarrollo.
- Se conserva la regla existente de cupos por rotación. No se rediseñó la gestión de capacidad simultánea entre rotaciones ni el historial de cambios de capacidad del servicio.
- El dashboard existente sigue mostrando asignaciones; desactivar una rotación no elimina ni cancela esas asignaciones.
- Persisten advertencias previas de tamaño de estilos. ExcelJS produce una advertencia CommonJS, sin impedir compilación.
- No se hizo commit ni push.
