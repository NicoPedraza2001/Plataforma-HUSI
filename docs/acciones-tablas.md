# Acciones de Programas, Universidades y Rotaciones

Actualización del 14 de septiembre de 2026.

Las tres tablas muestran exactamente tres acciones por registro: botón Activo/Inactivo, Editar y papelera sin texto visible (con nombre accesible y tooltip). El estado cambia sin diálogo y se actualiza con la respuesta del backend; si falla, conserva el estado anterior y muestra el error. Mientras se guarda se bloquean nuevas acciones.

Universidades usa Nombre, País, Asociados y Acciones. Ver programas está en Asociados. Rotaciones usa Asignatura/Servicio, Fechas, Cupos disponibles y Acciones. Los filtros de estado se mantienen. Programas lista activos e inactivos, también dentro de una universidad, para permitir reactivar desde la misma fila. La edición de programas inactivos conserva su estado.

La papelera abre una advertencia irreversible, ofrece Cancelar/Eliminar definitivamente y recomienda desactivar si se utilizará después. El backend bloquea el borrado de universidades con programas, programas con residentes y rotaciones con asignaciones, incluyendo relaciones inactivas/canceladas. No hay cascadas ni cambios de esquema.

Endpoints añadidos:
- PATCH /api/programas/{id}/estado: Activo o Inactivo; para activar exige universidad activa.
- DELETE /api/programas/{id}/definitivo: borrado físico si no hay residentes.
- DELETE /api/universidades/{id}/definitivo: borrado físico si no hay programas.

Se reutilizan PATCH /api/universidades/{id}/estado y los endpoints de Rotaciones. Se conservan los DELETE antiguos de programas/universidades como desactivación para no cambiar su contrato. Las nuevas papeleras utilizan explícitamente /definitivo.

Archivos: controllers, services y repositories de Programa/Universidad; EstadoProgramaRequest nuevo; ConsultaRepository; servicios HTTP de programas/universidades; componentes TS/HTML de los tres listados; CSS de universidades y admin.css; nuevo shared/icono-papelera; pruebas de acciones backend y frontend. Importación no modificada.

Verificación: build de producción aprobado; 25 pruebas backend y 39 frontend aprobadas. Incluyen cambio directo sin diálogo, respuesta fallida sin cambio falso, reactivación, cancelación/confirmación, lista de inactivos, borrado físico y bloqueo por relaciones. Revisión visual en localhost:4200. No se hizo commit ni push.
