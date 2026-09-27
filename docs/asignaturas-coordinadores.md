# Asignaturas y Coordinadores

Implementación en Desarrollo_Santi sobre el esquema existente; no requiere migraciones.

## Pantallas y organización

- `/admin/asignaturas`: listado, búsqueda por nombre o servicio, filtros de servicio y estado, paginación, formulario de creación/edición y confirmación de eliminación.
- `/admin/coordinadores`: listado, búsqueda por nombre, correo o servicio; los mismos filtros y acciones.
- `frontend/src/app/features/admin/asignaturas/` y `coordinadores/`: cada módulo separa `listado-*` y `formulario-*` en TypeScript, HTML, CSS y pruebas.
- `frontend/src/app/models/`: contratos Asignatura/Coordinador y sus Request; se reutiliza OpcionCatalogo.
- `frontend/src/app/services/`: clientes HTTP de cada dominio; los componentes no utilizan HttpClient.
- Backend: controller → service/interfaz → service/impl → repository. Entidades en model/entity, DTO en dto/request y dto/response, conversión en mapper.
- Se reutilizan Consulta, Paginacion, FocoModal, IconoPapelera y estilos compartidos. ConsultaController/ConsultaService/ConsultaRepository/ListadoSql resuelven búsquedas y paginación en PostgreSQL, con parámetros enlazados.

## Relaciones y reglas

- Asignatura pertenece a Servicio (`id_servicio`). Nombre obligatorio, máximo 200 caracteres. No se permite duplicar nombre dentro del mismo servicio, ignorando mayúsculas y espacios exteriores; puede repetirse en otro servicio.
- Si una asignatura tiene rotaciones, se bloquean su eliminación definitiva y el cambio de servicio para conservar el historial. Puede editarse su nombre o cambiar su estado.
- Coordinador relaciona Usuario con Servicio. La restricción existente `UNIQUE(id_usuario)` permite una sola asignación por usuario, incluso inactiva. Un servicio admite varios coordinadores.
- Para una nueva asignación, se ofrecen usuarios activos con rol Coordinador y sin asignación; se valida también en backend. Cambiar el rol de un usuario asociado exige retirar primero su asignación.
- Solo se ofrecen servicios activos al crear o cambiar relaciones. Al editar se conserva la referencia actual aunque se haya inactivado, sin obligar a reemplazarla.
- Activo/Inactivo se guarda inmediatamente sin modal, de forma independiente al estado del servicio/usuario. No se activan ni desactivan padres automáticamente.
- Eliminar un coordinador borra únicamente la relación: no borra Usuario ni Servicio. La advertencia lo explica.
- Los catálogos se consultan completos, sin truncarlos a la primera página del listado. No se exponen contraseñas.

## Endpoints

Prefijo `/api`. Para ambos recursos (`asignaturas`, `coordinadores`):

| Método | Ruta | Uso |
|---|---|---|
| GET | `/{recurso}?q=&servicioId=&estado=&pagina=1&tamano=10` | Búsqueda/filtros/paginación |
| GET | `/{recurso}/{id}` | Detalle para editar |
| POST | `/{recurso}` | Crear |
| PUT | `/{recurso}/{id}` | Editar |
| PATCH | `/{recurso}/{id}/estado` | Activo/Inactivo |
| DELETE | `/{recurso}/{id}/definitivo` | Borrado físico protegido |
| GET | `/servicios/catalogo?soloActivos=true` | Opciones de servicios; false incluye inactivos para filtros |
| GET | `/coordinadores/usuarios-disponibles` | Usuarios elegibles |

AsignaturaRequest: `{nombre, servicioId}`. CoordinadorRequest: `{usuarioId, servicioId}`. EstadoRequest: `{estado}`.

## Verificación

- Integración Spring/PostgreSQL con transacciones revertidas: crear, editar, buscar, filtrar, paginar, cambiar estado, eliminar, duplicados, referencias, usuarios/servicios inactivos y protección del rol.
- Angular: contratos HTTP, validación de formularios, listado vacío y errores de conexión.
- Pruebas manuales en localhost:4200 con registros sintéticos, lectura directa de PostgreSQL y limpieza posterior.
- Rotaciones fue reactivado posteriormente por solicitud del usuario; ver rotaciones.md. Se conserva su información existente.
