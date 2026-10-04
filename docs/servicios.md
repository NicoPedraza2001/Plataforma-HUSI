# CRUD de Servicios

Pantalla: /admin/servicios. Conserva búsqueda, filtro de estado y paginación en backend. Acciones: estado directo Activo/Inactivo, editar y papelera sin fondo con confirmación de eliminación definitiva.

## Organización

Frontend: formulario-servicio separado de listado-servicios dentro de features/admin/servicios; modelos en models/servicio.model.ts y HTTP en services/servicio.service.ts. Reutiliza modal, papelera, foco y estilos de formularios comunes.
Backend: ServicioController → ServicioService / ServicioServiceImpl → ServicioRepository, con entidad, DTOs y mapper propios.

## Endpoints

- GET /api/servicios: listado con q, estado, pagina y tamano.
- GET /api/servicios/{id}: detalle.
- POST /api/servicios: crear.
- PUT /api/servicios/{id}: editar.
- PATCH /api/servicios/{id}/estado: Activo/Inactivo.
- DELETE /api/servicios/{id}/definitivo: eliminación permanente.

## Validaciones

Nombre obligatorio hasta 150 caracteres y sin duplicados por mayúsculas/minúsculas. Descripción opcional. Cupos enteros no negativos: simultáneo <= máximo instalado. Se deshabilitó la conversión silenciosa de decimales a enteros en JSON para no aceptar cupos truncados.

Asignaturas o coordinadores asociados, incluso inactivos, bloquean el borrado. Desactivar no altera sus estados. Cambiar el cupo simultáneo recalcula las rotaciones vigentes del servicio (las finalizadas no se tocan) (cupo_disponible = cupo simultáneo − residentes Asignado) mediante el trigger trg_servicio_recalcular_cupo (database/init/13_recalcular_cupo_servicio.sql; en H2, CupoServicioTrigger). No se permite bajarlo por debajo del mayor número de asignados de una de sus rotaciones: ServicioServiceImpl responde 409 indicando el mínimo permitido y la base también lo rechaza. Si el cupo sube, los residentes en Esperando cupo de esas rotaciones se asignan solos por orden de llegada (ver docs/rotaciones.md, Lista de espera).

## Verificación

ServicioIntegrationTests cubre persistencia, búsqueda, edición, estados, eliminación, duplicados, cupos, asociaciones, recálculo de cupos de rotaciones y bloqueo al bajar el cupo por debajo de los asignados. Pruebas frontend verifican llamadas HTTP y validación del formulario. También se verificó un registro temporal creado y editado desde la interfaz contra PostgreSQL.
