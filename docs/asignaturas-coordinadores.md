# Asignaturas y Coordinadores

Implementación en Desarrollo_Santi sobre el esquema existente; no requiere migraciones.

## Pantallas y organización

- `/admin/asignaturas`: listado, búsqueda por nombre o servicio, filtros de servicio y estado, paginación, formulario de creación/edición y confirmación de eliminación.
- Los coordinadores ya no tienen pantalla propia: se crean y editan en `/admin/usuarios` (ver «Coordinadores» abajo).
- `frontend/src/app/features/admin/asignaturas/`: separa `listado-*` y `formulario-*` en TypeScript, HTML, CSS y pruebas.
- `frontend/src/app/models/`: contrato Asignatura y su Request; se reutiliza OpcionCatalogo.
- `frontend/src/app/services/`: clientes HTTP de cada dominio; los componentes no utilizan HttpClient.
- Backend: controller → service/interfaz → service/impl → repository. Entidades en model/entity, DTO en dto/request y dto/response, conversión en mapper.
- Se reutilizan Consulta, Paginacion, FocoModal, IconoPapelera y estilos compartidos. ConsultaController/ConsultaService/ConsultaRepository/ListadoSql resuelven búsquedas y paginación en PostgreSQL, con parámetros enlazados.

## Relaciones y reglas

- Asignatura pertenece a Servicio (`id_servicio`). Nombre obligatorio, máximo 200 caracteres. No se permite duplicar nombre dentro del mismo servicio, ignorando mayúsculas y espacios exteriores; puede repetirse en otro servicio.
- Si una asignatura tiene rotaciones, se bloquean su eliminación definitiva y el cambio de servicio para conservar el historial. Puede editarse su nombre o cambiar su estado.
- Solo se ofrecen servicios activos al crear o cambiar relaciones. Al editar se conserva la referencia actual aunque se haya inactivado, sin obligar a reemplazarla.
- Los catálogos se consultan completos, sin truncarlos a la primera página del listado. No se exponen contraseñas.

## Coordinadores

- Un coordinador es un usuario con rol Coordinador y un servicio asignado; en su perfil verá los residentes con rotaciones activas en ese servicio.
- Se crea y edita desde `/admin/usuarios`: al elegir el rol Coordinador, el formulario pide el servicio (`UsuarioRequest.servicioId`). Con otro rol el servicio se ignora.
- `UsuarioServiceImpl` mantiene la tabla `coordinador` (`UNIQUE(id_usuario)`): la crea o cambia de servicio al guardar, la retira si el usuario deja de ser coordinador o se elimina, y copia el estado Activo/Inactivo del usuario (el dashboard cuenta servicios sin coordinador activo).
- Por ahora un servicio admite varios coordinadores; la regla de uno por servicio está pendiente de definir con el hospital.
- El listado de usuarios muestra y busca por el servicio coordinado.

## Endpoints

Prefijo `/api`. Para `asignaturas`:

| Método | Ruta | Uso |
|---|---|---|
| GET | `/{recurso}?q=&servicioId=&estado=&pagina=1&tamano=10` | Búsqueda/filtros/paginación |
| GET | `/{recurso}/{id}` | Detalle para editar |
| POST | `/{recurso}` | Crear |
| PUT | `/{recurso}/{id}` | Editar |
| PATCH | `/{recurso}/{id}/estado` | Activo/Inactivo |
| DELETE | `/{recurso}/{id}/definitivo` | Borrado físico protegido |
| GET | `/servicios/catalogo?soloActivos=true` | Opciones de servicios; false incluye inactivos para filtros |

AsignaturaRequest: `{nombre, servicioId}`. UsuarioRequest (coordinadores): `{nombre, apellido, correo, rolId, contrasena, servicioId}`. EstadoRequest: `{estado}`.

## Verificación

- Integración Spring/PostgreSQL con transacciones revertidas: crear, editar, buscar, filtrar, paginar, cambiar estado, eliminar, duplicados, referencias, usuarios/servicios inactivos y protección del rol.
- Angular: contratos HTTP, validación de formularios, listado vacío y errores de conexión.
- Pruebas manuales en localhost:4200 con registros sintéticos, lectura directa de PostgreSQL y limpieza posterior.
- Rotaciones fue reactivado posteriormente por solicitud del usuario; ver rotaciones.md. Se conserva su información existente.
