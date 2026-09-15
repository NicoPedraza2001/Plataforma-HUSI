# Universidades y su relación con Programas

Implementación y verificación: 13 de septiembre de 2026. Continúa la integración frontend–backend anterior en Desarrollo_santiago.

## Resultado

La nueva pantalla está en `/admin/universidades`, accesible desde el menú. Lista, busca y filtra universidades con paginación del backend; permite crear, editar, desactivar, reactivar y entrar a sus programas. Cada operación vuelve a consultar al servidor. Usa los estilos existentes y la capa `src/app/services`.

La relación encontrada es Universidad 1 → N Programa. La FK obligatoria es `programa.id_universidad`; JPA ya tenía `Programa.universidad` con `@ManyToOne`. No se cambió esa relación ni se modificó el esquema SQL.

Se completó el mapeo de `Universidad` con los campos existentes `pais` y `perfil`. Nombre (150) y país (100) son obligatorios; perfil (50) es opcional. Los textos se recortan, se validan sus límites y se rechazan nombres duplicados, incluyendo universidades inactivas.

## Estado y conservación

Una universidad nueva queda Activa. Editar sus datos conserva su estado. Desactivar cambia únicamente `universidad.estado` a Inactivo. No elimina filas ni cambia los estados de programas o residentes. Puede reactivarse desde la misma pantalla. Los registros con estado Eliminado preexistente no se exponen ni reactivan mediante este CRUD.

Se conserva la regla anterior de Programas: no permite crear programas en una universidad inactiva. El acceso desde una universidad inactiva permite consultar los programas activos conservados y explica por qué crear/editar está deshabilitado allí. La desactivación de programas sigue siendo lógica.

## Universidad → Programas

`Ver programas` abre `/admin/universidades/:universidadId/programas` reutilizando el componente de Programas, no otra implementación de su CRUD.

- La consulta utiliza el ID, no el nombre de la universidad.
- Búsqueda y cambio de página conservan ese alcance y consultan al servidor.
- Renombrar una universidad no rompe sus asociaciones.
- El servidor hace prevalecer el ID de la ruta sobre cualquier parámetro recibido.
- Al crear o editar desde esta vista, la universidad queda fijada; en el listado general se conserva la selección existente.
- Si no hay programas activos se muestra un mensaje explícito.

## Endpoints

Todos usan prefijo `/api`.

| Método / ruta | Comportamiento |
| --- | --- |
| GET /universidades | Catálogo de activas, compatible con los formularios existentes; ahora delega a UniversidadService |
| GET /universidades/buscar | Listado completo paginado, búsqueda por nombre/país/perfil y filtro de estado |
| GET /universidades/{id} | Datos completos de la universidad; 404 cuando no está disponible |
| POST /universidades | Crea una universidad; 201 |
| PUT /universidades/{id} | Edita nombre, país y perfil conservando estado |
| DELETE /universidades/{id} | Desactiva lógicamente; 204 |
| PATCH /universidades/{id}/estado | Activo o Inactivo; devuelve la universidad actualizada |
| GET /universidades/{id}/programas | Programas activos de esa universidad, paginados |
| GET /programas/buscar | Conservado; añade el filtro opcional universidadId |

Parámetros habituales: `q`, `pagina` (desde 1), `tamano` (1–100); universidades añade `estado`. Los parámetros se validan y se enlazan a consultas SQL. Errores de campos: 400; duplicados: 409; universidad inexistente: 404. Se conservan todos los endpoints de creación, edición y desactivación de Programas.

## Importación

Se retiraron de Programas el botón Importar Excel, el estado que abría el asistente y su montaje en la pantalla. La nueva pantalla de Universidades no tiene importación. Se retiraron los estilos usados exclusivamente por ese botón.

Se conservaron `excel-programas.ts`, el componente `importar-programas`, ExcelJS, las funciones de importación del servicio y los endpoints de validación/carga. Quedan disponibles para la futura carga inicial y adaptación a Rotaciones. No existía una pantalla funcional de Rotaciones en las rutas actuales y no se construyó una en esta etapa.

## Pruebas

- Backend ejecutado en localhost:8080; frontend en localhost:4200; PostgreSQL mediante Docker.
- Compilación final correcta del frontend y backend.
- Backend: 13 pruebas aprobadas (4 nuevas de universidades, 9 conservadas).
- Frontend: 30 pruebas aprobadas (4 nuevas de universidades/programas asociados, 26 conservadas).
- Nuevas pruebas backend: persistencia de campos, edición conservando estado, duplicados incluso inactivos, obligatorios y longitudes, estados inválidos, universidad inexistente, desactivación sin cascadas, bloqueo de programas para universidades inactivas, reactivación, paginación por universidad y asociación estable tras renombrarla.
- Nuevas pruebas frontend: enlace por ID, ausencia de importación, obligatorios, POST real mediante servicio, recarga del listado, confirmación de desactivación, errores del backend y creación de programa con universidad fijada.
- Prueba real en navegador: guardar bloqueado sin obligatorios; creación y edición de universidad; mensaje de duplicado; acceso a lista vacía de programas; creación y edición de programa; desactivación y reactivación de universidad; consulta de programas conservados; desactivación final del programa de prueba.
- Comprobación SQL directa: universidad de prueba ID 35 guardada y editada; programa de prueba ID 123 vinculado por FK al ID 35. La consulta de otra universidad no lo devuelve. Al desactivar universidad, se comprobó `universidad=Inactivo` y `programa=Activo`, sin perder la relación.
- HTTP real: formulario vacío devuelve 400 con errores por nombre/país; crear programa en universidad inactiva devuelve 400 explicativo.

Los registros usados en la prueba de navegador se dejaron inactivos: universidad 35 y programa 123, identificados con el prefijo PRUEBA E2E. No se borraron físicamente. Las inserciones de las pruebas automatizadas se revierten con transacciones.

## Archivos de esta etapa

### Creados

- backend/src/main/java/co/edu/javeriana/husi/dto/request/UniversidadRequest.java
- backend/src/main/java/co/edu/javeriana/husi/dto/request/EstadoUniversidadRequest.java
- backend/src/main/java/co/edu/javeriana/husi/dto/response/UniversidadResponse.java
- backend/src/main/java/co/edu/javeriana/husi/mapper/UniversidadMapper.java
- backend/src/main/java/co/edu/javeriana/husi/service/UniversidadService.java
- backend/src/main/java/co/edu/javeriana/husi/service/impl/UniversidadServiceImpl.java
- backend/src/test/java/co/edu/javeriana/husi/service/UniversidadIntegrationTests.java
- frontend/src/app/services/universidades-api.ts
- frontend/src/app/features/admin/universidades/listado-universidades/listado-universidades.ts
- frontend/src/app/features/admin/universidades/listado-universidades/listado-universidades.html
- frontend/src/app/features/admin/universidades/listado-universidades/listado-universidades.css
- frontend/src/app/features/admin/universidades/listado-universidades/listado-universidades.spec.ts
- frontend/src/app/features/admin/programas/listado-programas/programas-universidad.spec.ts
- docs/universidades-programas.md

### Modificados

- backend/src/main/java/co/edu/javeriana/husi/model/entity/Universidad.java
- backend/src/main/java/co/edu/javeriana/husi/repository/UniversidadRepository.java
- backend/src/main/java/co/edu/javeriana/husi/controller/UniversidadController.java
- backend/src/main/java/co/edu/javeriana/husi/repository/ConsultaRepository.java
- backend/src/main/java/co/edu/javeriana/husi/service/impl/ConsultaServiceImpl.java
- backend/src/main/java/co/edu/javeriana/husi/service/ProgramaService.java
- backend/src/main/java/co/edu/javeriana/husi/service/impl/ProgramaServiceImpl.java
- frontend/src/app/app.routes.ts
- frontend/src/app/layout/admin/admin-layout/admin-layout.ts
- frontend/src/app/services/programas-api.ts
- frontend/src/app/features/admin/programas/listado-programas/listado-programas.ts
- frontend/src/app/features/admin/programas/listado-programas/listado-programas.html
- frontend/src/app/features/admin/programas/listado-programas/listado-programas.css
- frontend/src/app/features/admin/programas/listado-programas/listado-programas.spec.ts
- docs/integracion-frontend-backend.md (referencia a esta actualización)

No se eliminaron archivos en esta etapa.

## Pendientes existentes

No quedan fallos detectados en el flujo CRUD verificado. Continúan pendientes de otras etapas la autenticación/auditoría por usuario y la pantalla de Rotaciones con su importación. La compilación conserva advertencias de tamaño de estilos de pantallas anteriores; no impiden compilar. Los cambios quedan locales, sin commit ni push.
