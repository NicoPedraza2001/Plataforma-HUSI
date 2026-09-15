# Integración frontend–backend

> Actualización posterior: el CRUD de Universidades y los cambios de importación se documentan en [Universidades y Programas](universidades-programas.md).

Revisión e implementación: 13 de septiembre de 2026, rama Desarrollo_santiago.

## Resultado

Programas, Residentes, Servicios, Usuarios e Historial consultan PostgreSQL mediante el backend al abrir la pantalla, buscar, filtrar y cambiar de página. El Dashboard utiliza agregados calculados en PostgreSQL y una consulta paginada para próximas asignaciones. Los catálogos de filtros también vienen del servidor, no de las filas de la página visible.

Se conservaron Angular standalone, señales, los estilos y componentes existentes, las capas controller → service → repository de Spring Boot y el esquema de PostgreSQL. Las nuevas proyecciones de lectura usan EntityManager con SQL nativo parametrizado dentro de repositorios; las escrituras de Programas continúan usando las entidades y repositorios JPA existentes. No se modificaron las tablas ni los datos de negocio.

No se encontró un archivo o implementación llamado resApi/resapi. El cliente equivalente era programas-api.ts, que se trasladó desde la feature a services.

## Capa de servicios del frontend

En frontend/src/app/services:

- programas-api.ts: CRUD, consulta paginada, catálogo de universidades e importación existentes.
- residentes-api.ts, servicios-api.ts, usuarios-api.ts, historial-api.ts, rotaciones-api.ts: consultas por dominio y tipos de respuesta.
- dashboard-api.ts: métricas calculadas por el backend.
- catalogos-api.ts: opciones completas de universidades, programas, servicios, roles, periodos, módulos y acciones.
- sesion-api.ts: consulta del estado de disponibilidad del perfil.
- consulta.ts: estado reutilizable de carga, datos y error; cancelación con switchMap; espera de 300 ms al buscar; cancelación al salir de la pantalla; reintento sin conservar filas de una consulta fallida.
- api-error.ts: mensajes comunes de conexión y errores del servidor.
- consulta.spec.ts: pruebas de parámetros enviados, cancelación, espera, errores y recuperación.

HttpClient aparece únicamente en esta capa, aparte de su registro global en app.config.ts y las pruebas. Los componentes invocan servicios, sin llamadas HTTP directas. No se introdujeron cachés de filas ni respuestas ficticias como respaldo.

## Endpoints

Todos tienen prefijo /api.

| Método y ruta | Parámetros / propósito |
| --- | --- |
| GET /programas/buscar | q, universidad, pagina, tamano; solo programas activos |
| GET /residentes | q, universidad, programa, estado, periodo, pagina, tamano |
| GET /servicios | q, estado, pagina, tamano |
| GET /usuarios | q, rol, estado, pagina, tamano; nunca contraseñas |
| GET /historial | q, modulo, accion, desde, hasta, pagina, tamano |
| GET /rotaciones | Asignaciones a residentes: q, universidad, servicio, estado, periodo, proximas=true, pagina, tamano |
| GET /universidades/buscar | q, estado, pagina, tamano |
| GET /catalogos | Opciones de filtros consultadas en PostgreSQL |
| GET /dashboard | periodo, universidad, servicio |
| GET /sesion/perfil | Devuelve 401 explicativo mientras no exista autenticación; no presupone un usuario |

Respuesta paginada: contenido, totalElementos, totalPaginas, pagina, tamano. Las páginas comienzan en 1; tamaño permitido 1–100; búsqueda hasta 200 caracteres. Si la página queda fuera del total, el servidor devuelve la última disponible. Orden estable por nombre e ID; historial por fecha e ID; asignaciones por inicio e ID.

Los parámetros desconocidos, tamaños inválidos, periodos incorrectos y rangos de fechas invertidos producen 400. La búsqueda trata % y _ como texto literal; usa parámetros enlazados, no interpolación de valores. Los errores de acceso a la base devuelven 503 con mensaje seguro. Listas y totales comparten una transacción de lectura con snapshot consistente.

Se mantuvieron GET /programas, GET /programas/{id}, GET /universidades, POST /programas, PUT /programas/{id}, DELETE /programas/{id}, POST /programas/importacion/validar y POST /programas/importacion. El GET antiguo de Programas se conserva por compatibilidad; la pantalla utiliza /programas/buscar. DELETE sigue siendo desactivación lógica.

## Datos de demostración retirados

- Arrays de residentes, servicios, usuarios y registros de auditoría.
- Dashboard: factores artificiales por periodo/universidad, métricas inventadas, próximas rotaciones, médicos y alertas ficticias.
- Perfil y contraseña de ejemplo y mensajes de guardado local de Ajustes.
- Filtros y recortes de páginas locales de los listados; catálogos derivados de esos arrays.
- Métodos vacíos que simulaban botones de operaciones todavía no implementadas.

Se conserva el procesamiento local del Excel y la paginación del informe de importación: representan un archivo temporal y el resultado de una operación, no un listado de la BD. También se mantienen las listas estáticas de navegación, etiquetas de estado y cálculos gráficos de presentación.

## Significado de datos y métricas

- El periodo de un residente procede del semestre de inicio de sus asignaciones no canceladas; puede pertenecer a varios periodos. No es un nuevo campo académico inventado.
- Historial usa las acciones y módulos existentes. La BD no identifica separadamente el registro modificado: esa columna muestra —; el detalle conserva la información almacenada.
- Las métricas de rotaciones cuentan combinaciones distintas de asignatura, fecha de inicio y fecha de fin con residentes no cancelados. Distribución universitaria cuenta residentes distintos. La distribución ignora el filtro de universidad, como indicaba la pantalla.
- Rotaciones activas y ocupación se calculan para hoy en America/Bogota. Próximas abarca mañana hasta 30 días. La ocupación depende del servicio y del día actual, no del periodo ni de la universidad; su descripción lo indica.
- La BD no tiene doctor responsable por rotación, vencimientos de convenios, evaluaciones ni documentos pendientes. No se fabrican estos datos; el responsable muestra — y las alertas se basan en asignaciones reales en espera vigentes o futuras.

## Verificación realizada

- Frontend: build de producción exitoso. Suite completa: 26 pruebas aprobadas en 18 archivos.
- Backend: compilación y 9 pruebas aprobadas: contexto, 4 existentes de CRUD/importación/borrado lógico y 4 nuevas de consultas.
- Nuevas pruebas con PostgreSQL: búsqueda de un registro fuera de la primera página; páginas sin duplicados; combinación de filtros; límites; texto literal e intento de inyección; fechas inclusivas en horario de Bogotá; ausencia de contraseñas; catálogos y Dashboard. Las inserciones de prueba se revierten con transacciones.
- HTTP: rutas de todos los dominios responden 200; filtros de periodo/estado/fechas; próxima página; parámetros inválidos 400 y sesión no implementada 401.
- Proxy: peticiones desde localhost:4200/api llegan al backend en 8080.
- Navegador: segunda página de Programas, búsqueda Neurolog fuera de la página cargada, búsqueda sin coincidencias, Residentes por periodo 2025-2 y página 2, actualización del Dashboard, filtro de Servicios inactivos y Usuarios por rol.
- Totales contrastados con SQL directo: 41 programas activos, 214 residentes y 34 servicios. Son una captura de la base local al momento de verificar, no constantes de la aplicación.
- Docker se reinició durante una primera ejecución de pruebas; PostgreSQL se restableció y la suite del backend se repitió correctamente.

## Pendientes encontrados

- Autenticación y edición de perfil: no existen todavía en el backend original. Se retiró el perfil ficticio y Ajustes informa el motivo; no se añadió un login nuevo dentro de este refactor. Iniciar sesión en pgAdmin no inicia sesión en la plataforma.
- Crear/editar residentes, usuarios y servicios, permisos, importación de residentes y exportación de historial eran botones sin implementación. Se mantienen visibles, deshabilitados y señalados como pendientes. El CRUD/importador funcional de Programas se conserva.
- El menú Rotaciones apunta a una pantalla que todavía no existe en las rutas originales. El nuevo endpoint ya alimenta las próximas rotaciones del Dashboard; no se diseñó otra pantalla dentro de este alcance. Tampoco hay pantalla de universidades actualmente.
- El historial real está vacío y no existe todavía el registro de auditoría asociado a un usuario autenticado para las escrituras de Programas.
- Advertencias de compilación existentes: varios estilos exceden el presupuesto recomendado y ExcelJS usa CommonJS. No impiden compilar.

## Ejecución local

PostgreSQL en Docker, backend en localhost:8080/api y frontend en http://localhost:4200/admin/programas. No es necesario mantener pgAdmin abierto para que la aplicación consulte la base.

## Inventario de archivos

### Creados

- `backend/src/main/java/co/edu/javeriana/husi/controller/ConsultaController.java`
- `backend/src/main/java/co/edu/javeriana/husi/controller/DashboardController.java`
- `backend/src/main/java/co/edu/javeriana/husi/controller/SesionController.java`
- `backend/src/main/java/co/edu/javeriana/husi/dto/response/PaginaResponse.java`
- `backend/src/main/java/co/edu/javeriana/husi/repository/ConsultaRepository.java`
- `backend/src/main/java/co/edu/javeriana/husi/repository/DashboardRepository.java`
- `backend/src/main/java/co/edu/javeriana/husi/service/ConsultaService.java`
- `backend/src/main/java/co/edu/javeriana/husi/service/DashboardService.java`
- `backend/src/main/java/co/edu/javeriana/husi/service/impl/ConsultaServiceImpl.java`
- `backend/src/main/java/co/edu/javeriana/husi/service/impl/DashboardServiceImpl.java`
- `backend/src/test/java/co/edu/javeriana/husi/service/ConsultaIntegrationTests.java`
- `docs/integracion-frontend-backend.md`
- `frontend/src/app/services/api-error.ts`
- `frontend/src/app/services/catalogos-api.ts`
- `frontend/src/app/services/consulta.spec.ts`
- `frontend/src/app/services/consulta.ts`
- `frontend/src/app/services/dashboard-api.ts`
- `frontend/src/app/services/historial-api.ts`
- `frontend/src/app/services/programas-api.ts`
- `frontend/src/app/services/residentes-api.ts`
- `frontend/src/app/services/rotaciones-api.ts`
- `frontend/src/app/services/servicios-api.ts`
- `frontend/src/app/services/sesion-api.ts`
- `frontend/src/app/services/usuarios-api.ts`

### Modificados

- `backend/src/main/java/co/edu/javeriana/husi/exception/ApiExceptionHandler.java`
- `frontend/src/app/app.spec.ts`
- `frontend/src/app/features/admin/ajustes/ajustes.html`
- `frontend/src/app/features/admin/ajustes/ajustes.spec.ts`
- `frontend/src/app/features/admin/ajustes/ajustes.ts`
- `frontend/src/app/features/admin/dashboard/panel-dashboard/panel-dashboard.html`
- `frontend/src/app/features/admin/dashboard/panel-dashboard/panel-dashboard.spec.ts`
- `frontend/src/app/features/admin/dashboard/panel-dashboard/panel-dashboard.ts`
- `frontend/src/app/features/admin/historial/historial.html`
- `frontend/src/app/features/admin/historial/historial.spec.ts`
- `frontend/src/app/features/admin/historial/historial.ts`
- `frontend/src/app/features/admin/programas/excel-programas.ts`
- `frontend/src/app/features/admin/programas/importar-programas/importar-programas.ts`
- `frontend/src/app/features/admin/programas/listado-programas/listado-programas.html`
- `frontend/src/app/features/admin/programas/listado-programas/listado-programas.spec.ts`
- `frontend/src/app/features/admin/programas/listado-programas/listado-programas.ts`
- `frontend/src/app/features/admin/residentes/listado-residentes/listado-residentes.html`
- `frontend/src/app/features/admin/residentes/listado-residentes/listado-residentes.spec.ts`
- `frontend/src/app/features/admin/residentes/listado-residentes/listado-residentes.ts`
- `frontend/src/app/features/admin/servicios/listado-servicios/listado-servicios.html`
- `frontend/src/app/features/admin/servicios/listado-servicios/listado-servicios.spec.ts`
- `frontend/src/app/features/admin/servicios/listado-servicios/listado-servicios.ts`
- `frontend/src/app/features/admin/usuarios/listado-usuarios/listado-usuarios.html`
- `frontend/src/app/features/admin/usuarios/listado-usuarios/listado-usuarios.spec.ts`
- `frontend/src/app/features/admin/usuarios/listado-usuarios/listado-usuarios.ts`
- `frontend/src/app/features/public/landing/pages/landing/landing.spec.ts`
- `frontend/src/app/layout/admin/admin-layout/admin-layout.spec.ts`
- `frontend/src/app/layout/public/public-layout/public-layout.spec.ts`

### Eliminados o trasladados

- `frontend/src/app/features/admin/dashboard/datos-demo.ts`
- `frontend/src/app/features/admin/programas/programas-api.ts`
