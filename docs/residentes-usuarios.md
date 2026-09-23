# Residentes y usuarios

Los dos módulos tienen búsqueda y paginación en backend, creación, edición, cambio directo Activo/Inactivo y eliminación definitiva con confirmación. Cada formulario está separado de su listado en `features/admin/<módulo>/formulario-<entidad>/`. Las llamadas HTTP viven en `services` y los contratos en `models`.

## API

- GET /api/residentes y /api/usuarios: listados con filtros y paginación existentes.
- GET /api/residentes/{id} y /api/usuarios/{id}: detalle editable.
- POST /api/residentes y /api/usuarios: creación.
- PUT /api/residentes/{id} y /api/usuarios/{id}: edición.
- PATCH /api/residentes/{id}/estado y /api/usuarios/{id}/estado: Activo/Inactivo.
- DELETE /api/residentes/{id}/definitivo y /api/usuarios/{id}/definitivo: eliminación permanente si no hay referencias.
- GET /api/roles: catálogo de roles reales; GET /api/programas: catálogo para residentes.

## Reglas

El residente requiere nombre, tipo y número de documento y programa. Documento único. Se valida que las nuevas asignaciones sean a programas y universidades activos. Se permite conservar el programa actual al editar aunque haya sido desactivado. Las rotaciones asociadas bloquean la eliminación definitiva.

El usuario requiere nombre, apellido, correo, rol y contraseña inicial de al menos ocho caracteres. Las contraseñas se almacenan como hash BCrypt (máximo 72 bytes); nunca se devuelven en las respuestas. Contraseña vacía al editar conserva la actual. Correo único sin distinguir mayúsculas. Coordinación y auditoría bloquean la eliminación.

La autenticación y autorización del proyecto siguen pendientes como antes; este CRUD no implementa inicio de sesión ni control de acceso por rol.

## Rotaciones

Se retiraron pantallas, formularios, importador y endpoints de gestión de Rotaciones. Se conservaron las tablas, datos, scripts de base de datos y la consulta histórica de solo lectura usada por Dashboard. Excel compartido permanece disponible para usos futuros. La ruta antigua de pantalla redirige a Residentes.

## Pruebas

Backend: `./mvnw.cmd test` con PostgreSQL local; PersonasIntegrationTests verifica persistencia, edición, estados, duplicados, hash de contraseña y bloqueo de borrado asociado, con rollback.
Frontend: `npm test -- --watch=false` y `npm run build`.
